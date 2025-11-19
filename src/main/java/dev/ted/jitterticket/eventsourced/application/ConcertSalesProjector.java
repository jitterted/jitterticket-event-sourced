package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadata;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadataRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertSalesProjector {

    static final String PROJECTION_NAME = "concert_sales_projector";

    private final Map<ConcertId, ConcertSalesSummary> salesSummaryMap = new HashMap<>();

    private final ProjectionMetadataRepository projectionMetadataRepository;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    private ConcertSalesProjector(ProjectionMetadataRepository projectionMetadataRepository, EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.projectionMetadataRepository = projectionMetadataRepository;
        this.concertEventStore = concertEventStore;
        ProjectionMetadata projectionMetadata =
                projectionMetadataRepository.findById(PROJECTION_NAME)
                                            .orElse(new ProjectionMetadata(PROJECTION_NAME, 0L));
        concertEventStore.subscribe(this, projectionMetadata.getLastGlobalEventSequenceSeen());
    }

    public static ConcertSalesProjector createNew(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        return new ConcertSalesProjector(null, concertEventStore);
    }

    @Deprecated // require the ProjectionMetadataRepository or create an in-memory version
    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        ConcertSalesProjector concertSalesProjector =
                new ConcertSalesProjector(null, concertEventStore);
        return concertSalesProjector;
    }

    public static ConcertSalesProjector createForTest() {
        return createForTest(InMemoryEventStore.forConcerts());
    }

    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                                                      ProjectionMetadataRepository projectionMetadataRepository) {
        ConcertSalesProjector concertSalesProjector =
                new ConcertSalesProjector(projectionMetadataRepository, concertEventStore);
        return concertSalesProjector;
    }

    public Stream<ConcertSalesSummary> allSalesSummaries() {
        return salesSummaryMap.values().stream();
    }

    public void apply(Stream<ConcertEvent> concertEvents) {
        concertEvents
                .forEach(concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled concertScheduled -> salesSummaryMap.put(
                                concertScheduled.concertId(),
                                createSummaryFrom(concertScheduled));
                        case TicketsSold ticketsSold -> salesSummaryMap.computeIfPresent(
                                ticketsSold.concertId(),
                                (_, summary) -> summary.plusTicketsSold(ticketsSold));
                        case ConcertRescheduled concertRescheduled -> salesSummaryMap.computeIfPresent(
                                concertRescheduled.concertId(),
                                (_, summary) -> summary.withNewShowDateTime(concertRescheduled.newShowDateTime()));
                    }
                });
    }

    private static ConcertSalesSummary createSummaryFrom(ConcertScheduled concertScheduled) {
        return new ConcertSalesSummary(
                concertScheduled.concertId(),
                concertScheduled.artist(),
                concertScheduled.showDateTime(),
                0, // tickets sold
                0  // ticket $$ sales
        );
    }


    public record ConcertSalesSummary(
            ConcertId concertId,
            String artist,
            LocalDateTime showDateTime,
            int totalQuantity,
            int totalSales
    ) {
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
