package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadata;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadataRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.List;
import java.util.stream.Stream;

public class Projections {

    private final ConcertSalesProjector concertSalesProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;
    private final ProjectionMetadataRepository projectionMetadataRepository;
    private final ConcertSalesProjectionRepository concertSalesProjectionRepository;

    public Projections(ConcertSalesProjector concertSalesProjector,
                       EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                       ProjectionMetadataRepository projectionMetadataRepository,
                       ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        this.concertSalesProjector = concertSalesProjector;
        this.concertEventStore = concertEventStore;
        this.projectionMetadataRepository = projectionMetadataRepository;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;

        this.ensureMetadataExistsIn(projectionMetadataRepository);

        long lastGlobalEventSequenceSeen = this.projectionMetadataRepository
                .lastGlobalEventSequenceSeenByProjectionName(ConcertSalesProjector.PROJECTION_NAME)
                .orElse(0L);
        List<ConcertSalesProjector.ConcertSalesSummary> concertSalesSummaries
                = this.catchUpForAllEventsInEventStore(lastGlobalEventSequenceSeen).toList();
        updatePersistentProjection(projectionMetadataRepository,
                                   concertSalesProjectionRepository,
                                   concertSalesSummaries);

        concertEventStore.subscribe(
                concertSalesProjector, lastGlobalEventSequenceSeen);
    }

    private static void updatePersistentProjection(ProjectionMetadataRepository projectionMetadataRepository, ConcertSalesProjectionRepository concertSalesProjectionRepository, List<ConcertSalesProjector.ConcertSalesSummary> concertSalesSummaries) {
        concertSalesSummaries.forEach(css -> {
            // TODO: improve how we do this: upsert instead of delete/insert
            concertSalesProjectionRepository.deleteById(
                    css.concertId().id());
            concertSalesProjectionRepository.save(
                    ConcertSalesProjection.createFromSummary(css));
        });
        if (!concertSalesSummaries.isEmpty()) {
            ProjectionMetadata projectionMetadata = new ProjectionMetadata();
            projectionMetadata.setProjectionName(ConcertSalesProjector.PROJECTION_NAME);
            projectionMetadata.setLastGlobalEventSequenceSeen(1L);
            projectionMetadataRepository.save(projectionMetadata);
        }
    }

    private void ensureMetadataExistsIn(ProjectionMetadataRepository projectionMetadataRepository) {
        if (!projectionMetadataRepository
                .existsByProjectionName(ConcertSalesProjector.PROJECTION_NAME)) {
            projectionMetadataRepository.saveIfNotExist(
                    ConcertSalesProjector.PROJECTION_NAME, 0L);
        }
    }

    public Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries() {
        return concertSalesProjectionRepository
                .findAll()
                .stream()
                .map(ConcertSalesProjection::toSummary);
    }

    private Stream<ConcertSalesProjector.ConcertSalesSummary> catchUpForAllEventsInEventStore(long lastGlobalEventSequenceSeen) {
        Stream<ConcertEvent> concertEventStream =
                concertEventStore.allEventsAfter(lastGlobalEventSequenceSeen);
        List<ConcertSalesProjection> loadedProjectionRows =
                concertSalesProjectionRepository.findAll();
        return concertSalesProjector.project(loadedProjectionRows,
                                             concertEventStream
                                    )
                                    .stream()
                                    .map(ConcertSalesProjection::toSummary);
    }
}
