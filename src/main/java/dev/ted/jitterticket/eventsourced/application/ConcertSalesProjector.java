package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionDbo;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcertSalesProjector {

    static final String PROJECTION_NAME = "concert_sales_projector";

    // class ProjectorMediator (depends on EventStore)
    //     sends events (uncommitted ones that were just persisted) to...
    //     class ConcertSalesProjectionMediator (depends on Projection & Metadata Repositories)
    //         load last event sequence (from metadata repo)
    //         load projection rows from database (from projection repo)
    //              ==> dispatch to Projector.project(rows, events)
    //         save updated projection rows (to projection repo)
    //         save last event sequence (to metadata repo)

    // TODO: project should not be aware of Database Objects (DBO)
    public ConcertSalesProjectionDbo project(
            ConcertSalesProjectionDbo loadedProjectionDbo,
            Stream<ConcertEvent> concertEvents) {

        Map<ConcertId, ConcertSalesSummary> salesSummaryMap =
                loadedProjectionDbo.getConcertSales()
                                   .stream()
                                   .map(ConcertSalesProjector::dboToSummary)
                                   .collect(Collectors.toMap(ConcertSalesSummary::concertId,
                                                             Function.identity()));

        ProjectionResult result = computeProjection(concertEvents,
                                                    salesSummaryMap);

        Set<ConcertSalesDbo> concertSalesDbos =
                result.salesSummaries()
                      .stream()
                      .map(this::summaryToDbo)
                      .collect(Collectors.toUnmodifiableSet());

        loadedProjectionDbo.setLastEventSequenceSeen(
                result.lastEventSequenceSeen());
        loadedProjectionDbo.setConcertSales(concertSalesDbos);
        return loadedProjectionDbo;
    }

    static ConcertSalesSummary dboToSummary(ConcertSalesDbo concertSalesDbo) {
        return new ConcertSalesSummary(
                new ConcertId(concertSalesDbo.getConcertId()),
                concertSalesDbo.getArtistName(),
                concertSalesDbo.getConcertDate().atStartOfDay(),
                concertSalesDbo.getTicketsSold(),
                concertSalesDbo.getTotalSales()
        );
    }

    private ConcertSalesDbo summaryToDbo(ConcertSalesSummary summary) {
        return new ConcertSalesDbo(
                summary.concertId().id(),
                summary.artist(),
                summary.showDateTime().toLocalDate(),
                summary.totalQuantity(),
                summary.totalSales()
        );
    }

    private static ProjectionResult computeProjection(Stream<ConcertEvent> concertEvents, Map<ConcertId, ConcertSalesSummary> salesSummaryMap) {
        AtomicReference<Long> lastEventSequenceSeenRef = new AtomicReference<>(0L);
        concertEvents
                .forEach(concertEvent -> {
                    if (concertEvent.eventSequence() == null) {
                        throw new IllegalStateException("Event sequence cannot be null for event: " + concertEvent);
                    }
                    switch (concertEvent) {
                        case ConcertScheduled concertScheduled -> salesSummaryMap.put(
                                concertScheduled.concertId(),
                                ConcertSalesSummary.createSummaryFrom(concertScheduled));
                        case TicketsSold ticketsSold -> salesSummaryMap.computeIfPresent(
                                ticketsSold.concertId(),
                                (_, summary) -> summary.plusTicketsSold(ticketsSold));
                        case ConcertRescheduled concertRescheduled -> salesSummaryMap.computeIfPresent(
                                concertRescheduled.concertId(),
                                (_, summary) -> summary.withNewShowDateTime(concertRescheduled.newShowDateTime()));
                    }
                    lastEventSequenceSeenRef.set(concertEvent.eventSequence());
                });
        Collection<ConcertSalesSummary> salesSummaries = salesSummaryMap.values();
        long lastEventSequenceSeen = lastEventSequenceSeenRef.get();
        return new ProjectionResult(salesSummaries, lastEventSequenceSeen);
    }

    private record ProjectionResult(Collection<ConcertSalesSummary> salesSummaries,
                                    long lastEventSequenceSeen) {}


    public record ConcertSalesSummary(
            ConcertId concertId,
            String artist,
            LocalDateTime showDateTime,
            int totalQuantity,
            int totalSales
    ) {
        private static ConcertSalesSummary createSummaryFrom(ConcertScheduled concertScheduled) {
            return new ConcertSalesSummary(
                    concertScheduled.concertId(),
                    concertScheduled.artist(),
                    concertScheduled.showDateTime(),
                    0, // tickets sold
                    0  // ticket $$ sales
            );
        }

        public ConcertSalesSummary plusTicketsSold(TicketsSold ticketsSold) {
            return new ConcertSalesSummary(
                    concertId, artist,
                    showDateTime,
                    totalQuantity + ticketsSold.quantity(),
                    totalSales + ticketsSold.totalPaid());
        }

        public ConcertSalesSummary withNewShowDateTime(LocalDateTime newShowDateTime) {
            return new ConcertSalesSummary(
                    concertId, artist,
                    newShowDateTime,
                    totalQuantity, totalSales);
        }
    }
}
