package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.stream.Stream;

public class NewProjectionCoordinator<STATE, DELTA extends ProjectionDelta>
        implements EventStreamConsumer {

    private final NewDomainProjector<STATE, DELTA> domainProjector;
    private final ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort;
    private STATE cachedProjection;
    private Checkpoint lastWrittenCheckpoint;

    public NewProjectionCoordinator(NewDomainProjector<STATE, DELTA> domainProjector,
                                    ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort,
                                    EventStore<?, ? extends Event, ?> eventStore) {
        this.domainProjector = domainProjector;
        this.projectionPersistencePort = projectionPersistencePort;

        var snapshot = projectionPersistencePort.loadSnapshot();
        cachedProjection = snapshot.state();
        lastWrittenCheckpoint = snapshot.checkpoint();

        Stream<? extends Event> eventStream = eventStore.allEventsAfter(lastWrittenCheckpoint);
        handle(eventStream);

        eventStore.subscribe(this);
    }

    @Override
    public void handle(Stream<? extends Event> eventStream) {
        domainProjector.handle(eventStream);
        Checkpointed<DELTA> checkpointedDelta = domainProjector.flush();

        if (checkpointedDelta.checkpoint().newerThan(lastWrittenCheckpoint)) {
            lastWrittenCheckpoint = checkpointedDelta.checkpoint();
            projectionPersistencePort.saveDelta(
                    new Checkpointed<>(checkpointedDelta.state(), checkpointedDelta.checkpoint()));
        }
    }

    public STATE projection() {
        return domainProjector.projection().state();
    }
}
