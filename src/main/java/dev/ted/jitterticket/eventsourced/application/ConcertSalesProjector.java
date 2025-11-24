package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertSalesProjector {

    static final String PROJECTION_NAME = "concert_sales_projector";

    private final Map<ConcertId, ConcertSalesSummary> salesSummaryMap = new HashMap<>();

    private ProjectionMetadataRepository projectionMetadataRepository;
    private ConcertSalesProjectionRepository concertSalesProjectionRepository;
    private EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    public ConcertSalesProjector() {
    }

    private ConcertSalesProjector(ProjectionMetadataRepository projectionMetadataRepository,
                                  ConcertSalesProjectionRepository concertSalesProjectionRepository,
                                  EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.projectionMetadataRepository = projectionMetadataRepository;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;
        this.concertEventStore = concertEventStore;
        long lastGlobalEventSequenceSeen = projectionMetadataRepository
                .lastGlobalEventSequenceSeenByProjectionName(PROJECTION_NAME)
                .orElse(0L);
        concertEventStore.subscribe(this, lastGlobalEventSequenceSeen);
    }

    public static ConcertSalesProjector createNew(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        return new ConcertSalesProjector(null, null, concertEventStore);
    }

    //region Creation Methods for Testing
    @Deprecated // require the ProjectionMetadataRepository or create an in-memory version
    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        ConcertSalesProjector concertSalesProjector =
                new ConcertSalesProjector(null, null, concertEventStore);
        return concertSalesProjector;
    }

    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                                                      ConcertSalesProjectionRepository concertSalesProjectionRepository,
                                                      ProjectionMetadataRepository projectionMetadataRepository) {
        ConcertSalesProjector concertSalesProjector =
                new ConcertSalesProjector(projectionMetadataRepository, concertSalesProjectionRepository, concertEventStore);
        return concertSalesProjector;
    }

    public static ConcertSalesProjector createForTest(ProjectionMetadataRepository projectionMetadataRepository, ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        return new ConcertSalesProjector(
                projectionMetadataRepository,
                concertSalesProjectionRepository,
                InMemoryEventStore.forConcerts()
        );
    }
    //endregion

    public Stream<ConcertSalesSummary> allSalesSummaries() {
        return concertSalesProjectionRepository
                .findAll()
                .stream()
                .map(ConcertSalesProjection::toSummary);
    }

    // class ProjectionEventHandler
    //   #register(Projector, ProjectionRepository, "projection_name", ConcertSalesProjection.class)
    //   void eventHandler(Stream<ConcertEvent> concertEvents, lastGlobalEventSequence)
    //     loadedProjectionRows = load ConcertSalesProjection from DB (or create the Projection in memory within a test)
    //              ProjectionRepository.findAll() -> List<Object> (raw list)
    //       projector.load(loadedProjectionRows) // transforms ConcertSalesProjection -> internal Map
    //       // applies events and transforms internal Map, returning the updated ConcertSalesProjection
    //       List<Object> updatedProjectionRows = projector.project(concertEvents) // "domain" aggregation/summarization logic
    //                                      Optimization: only return CHANGED "rows"
    //
    //       List<Object> updatedProjectionRows = projector.project(loadedProjectionRows, concertEvents)
    //     projectionRepository.saveAll(updatedProjectionRows)
    //     metadataRepository.save(projectionName, lastGlobalEventSequence)

    public List<ConcertSalesProjection> project(List<ConcertSalesProjection> loadedProjectionRows, Stream<ConcertEvent> concertEvents) {
        apply(concertEvents);
        return salesSummaryMap
                .values()
                .stream()
                .map(css -> new ConcertSalesProjection(
                        css.concertId().id(),
                        css.artist(),
                        css.showDateTime().toLocalDate(),
                        css.totalQuantity(),
                        css.totalSales()
                ))
                .toList();
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
