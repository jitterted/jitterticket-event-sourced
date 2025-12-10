package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConcertSalesProjector {

    static final String PROJECTION_NAME = "concert_sales_projector";

    @Deprecated // should be a local variable
    private Map<ConcertId, ConcertSalesSummary> salesSummaryMap = new HashMap<>();

    // class ProjectorMediator (depends on EventStore)
    //     sends events (uncommitted ones that were just persisted) to...
    //     class ConcertSalesProjectionMediator (depends on Projection & Metadata Repositories)
    //         load last global event sequence (from metadata repo)
    //         load projection rows from database (from projection repo)
    //              ==> call (dispatch to) Projector.project(rows, events)
    //         save updated projection rows (to projection repo)
    //         save last global event sequence (to metadata repo)

    public List<ConcertSalesProjection> project(List<ConcertSalesProjection> loadedProjectionRows,
                                                Stream<ConcertEvent> concertEvents) {
        loadPreviousProjection(loadedProjectionRows);
        apply(concertEvents);
        return salesSummaryMap
                .values()
                .stream()
                .map(ConcertSalesProjection::createFromSummary)
                .toList();
    }

    private void loadPreviousProjection(List<ConcertSalesProjection> loadedProjectionRows) {
        salesSummaryMap = loadedProjectionRows
                .stream()
                .map(ConcertSalesProjection::toSummary)
                .collect(Collectors.toMap(ConcertSalesSummary::concertId,
                                          Function.identity()));
    }

    @Deprecated // should be for internal use only
    public void apply(Stream<ConcertEvent> concertEvents) {
        concertEvents
                .forEach(concertEvent -> {
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
                });
    }


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
