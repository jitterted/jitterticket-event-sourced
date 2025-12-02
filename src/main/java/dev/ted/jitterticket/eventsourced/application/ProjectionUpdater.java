package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadataRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.Collections;
import java.util.stream.Stream;

public class ProjectionUpdater {

    private final ConcertSalesProjector concertSalesProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;
    private final ProjectionMetadataRepository projectionMetadataRepository;
    private final ConcertSalesProjectionRepository concertSalesProjectionRepository;

    public ProjectionUpdater(ConcertSalesProjector concertSalesProjector,
                             EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                             ProjectionMetadataRepository projectionMetadataRepository,
                             ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        this.concertSalesProjector = concertSalesProjector;
        this.concertEventStore = concertEventStore;
        this.projectionMetadataRepository = projectionMetadataRepository;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;

        ensureMetadataExistsIn(projectionMetadataRepository);

    }

    private static void ensureMetadataExistsIn(ProjectionMetadataRepository projectionMetadataRepository) {
        if (!projectionMetadataRepository
                .existsByProjectionName(ConcertSalesProjector.PROJECTION_NAME)) {
            projectionMetadataRepository.saveIfNotExist(
                    ConcertSalesProjector.PROJECTION_NAME, 0L);
        }
    }

    //region Creation Methods for Testing
    @Deprecated // require the ProjectionMetadataRepository or create an in-memory version
    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        return new ConcertSalesProjector(null, null, concertEventStore);
    }


    public static ConcertSalesProjector createForTest(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                                                      ConcertSalesProjectionRepository concertSalesProjectionRepository,
                                                      ProjectionMetadataRepository projectionMetadataRepository) {
        return new ConcertSalesProjector(projectionMetadataRepository,
                                         concertSalesProjectionRepository,
                                         concertEventStore);
    }

    /**
     * Supplies its own in-memory Event Store for Concerts
     */
    public static ConcertSalesProjector createForTest(ProjectionMetadataRepository projectionMetadataRepository, ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        return new ConcertSalesProjector(projectionMetadataRepository,
                                         concertSalesProjectionRepository,
                                         InMemoryEventStore.forConcerts()
        );
    }

    //endregion

    public Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries() {
        return catchUpForAllEventsInEventStore();
    }

    private Stream<ConcertSalesProjector.ConcertSalesSummary> catchUpForAllEventsInEventStore() {
        Stream<ConcertEvent> concertEventStream = concertEventStore.allEvents();
        return concertSalesProjector.project(Collections.emptyList(),
                                             concertEventStream
                                    )
                                    .stream()
                                    .map(ConcertSalesProjection::toSummary);
    }
}
