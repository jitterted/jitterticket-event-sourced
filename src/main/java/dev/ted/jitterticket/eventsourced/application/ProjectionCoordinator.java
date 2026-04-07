package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ProjectionCoordinator<EVENT extends Event, STATE, DELTA extends ProjectionDelta>
        implements EventStreamConsumer {

    private final DomainProjector<STATE, DELTA> domainProjector;
    private final ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort;
    private final EventStore<?, EVENT, ?> eventStore;
    private STATE cachedProjection;
    private Checkpoint cachedCheckpoint;

    public ProjectionCoordinator(DomainProjector<STATE, DELTA> domainProjector,
                                 ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort,
                                 EventStore<?, EVENT, ?> eventStore) {
        this.domainProjector = domainProjector;
        this.projectionPersistencePort = projectionPersistencePort;
        this.eventStore = eventStore;

        var snapshot = projectionPersistencePort.loadSnapshot();
        cachedProjection = snapshot.state();
        cachedCheckpoint = snapshot.checkpoint();

        Stream<EVENT> eventStream = eventStore.allEventsAfter(cachedCheckpoint);
        updateProjection(eventStream);

        eventStore.subscribe(this);
    }

    @Override
    public void handle(Stream<? extends Event> eventStream) {
        updateProjection(eventStream);
    }

    private void updateProjection(Stream<? extends Event> eventStream) {
        AtomicReference<Long> lastEventSeen = new AtomicReference<>(0L);
        Stream<? extends Event> checkpointTrackingEventStream = eventStream
                .peek(event -> lastEventSeen.set(event.eventSequence()));

        var projectorResult = domainProjector.project(
                cachedProjection,
                checkpointTrackingEventStream);

        cachedProjection = projectorResult.fullState();

        Checkpoint updatedCheckpoint = lastEventSeen.get() == 0L
                ? Checkpoint.INITIAL
                : Checkpoint.of(lastEventSeen.get());

        if (updatedCheckpoint.newerThan(cachedCheckpoint)) {
            cachedCheckpoint = updatedCheckpoint;
            projectionPersistencePort.saveDelta(
                    projectorResult.delta(),
                    updatedCheckpoint);
        }
    }

    public STATE projection() {
        return cachedProjection;
    }
}
