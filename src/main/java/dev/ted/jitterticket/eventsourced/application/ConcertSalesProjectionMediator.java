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

public class ConcertSalesProjectionMediator {

    private final ConcertSalesProjector concertSalesProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;
    private final ProjectionMetadataRepository projectionMetadataRepository;
    private final ConcertSalesProjectionRepository concertSalesProjectionRepository;

    public ConcertSalesProjectionMediator(ConcertSalesProjector concertSalesProjector,
                                          EventStore<ConcertId, ConcertEvent, Concert> concertEventStore,
                                          ProjectionMetadataRepository projectionMetadataRepository,
                                          ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        this.concertSalesProjector = concertSalesProjector;
        this.concertEventStore = concertEventStore;
        this.projectionMetadataRepository = projectionMetadataRepository;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;

        this.ensureMetadataExistsIn(projectionMetadataRepository);

        long lastGlobalEventSequenceSeenByThisProjection = this.projectionMetadataRepository
                .lastGlobalEventSequenceSeenByProjectionName(ConcertSalesProjector.PROJECTION_NAME)
                .orElse(0L);
        Stream<ConcertEvent> concertEventStream =
                this.concertEventStore.allEventsAfter(lastGlobalEventSequenceSeenByThisProjection);

        // handle needs the EventStore's lastGES so we know what the new checkpoint is
        handle(concertEventStream, lastGlobalEventSequenceSeenByThisProjection);

        concertEventStore.subscribe(this);
    }

    public void handle(Stream<ConcertEvent> concertEventStream, long lastGlobalEventSequenceSeen) {
        // load projection
        List<ConcertSalesProjection> loadedProjectionRows =
                this.concertSalesProjectionRepository.findAll();
        // update projection
        List<ConcertSalesProjector.ConcertSalesSummary> concertSalesSummaries
                = this.concertSalesProjector.project(loadedProjectionRows,
                                                     concertEventStream)
                                            .stream()
                                            .map(ConcertSalesProjection::toSummary).toList();
        // store updated projection
        concertSalesSummaries.forEach(css -> {
            // this will be replaced by a Projection "holder" entity that will have a list of the summary objects
            // and then the repository will handle the deletion/save of the individual summaries
            this.concertSalesProjectionRepository.deleteById(
                    css.concertId().id());
            this.concertSalesProjectionRepository.save(
                    ConcertSalesProjection.createFromSummary(css));
        });
        if (!concertSalesSummaries.isEmpty()) {
            ProjectionMetadata projectionMetadata = new ProjectionMetadata();
            projectionMetadata.setProjectionName(ConcertSalesProjector.PROJECTION_NAME);
            projectionMetadata.setLastGlobalEventSequenceSeen(lastGlobalEventSequenceSeen);
            this.projectionMetadataRepository.save(projectionMetadata);
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

}
