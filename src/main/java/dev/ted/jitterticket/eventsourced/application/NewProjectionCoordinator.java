package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class NewProjectionCoordinator<EVENT extends Event, STATE, DELTA extends ProjectionDelta>
        implements EventStreamConsumer {

    private final NewDomainProjector<STATE, DELTA> domainProjector;
    private final ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort;
    private STATE cachedProjection;
    private Checkpoint cachedCheckpoint;

    public NewProjectionCoordinator(NewDomainProjector<STATE, DELTA> domainProjector,
                                    ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort,
                                    EventStore<?, EVENT, ?> eventStore) {
        this.domainProjector = domainProjector;
        this.projectionPersistencePort = projectionPersistencePort;

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

        checkpointTrackingEventStream.toList();
//        var projectorResult = domainProjector.project(
//                cachedProjection,
//                checkpointTrackingEventStream);
//
//        cachedProjection = projectorResult.fullState();
//
        Checkpoint updatedCheckpoint = lastEventSeen.get() == 0L
                ? Checkpoint.INITIAL
                : Checkpoint.of(lastEventSeen.get());

        if (updatedCheckpoint.newerThan(cachedCheckpoint)) {
            cachedCheckpoint = updatedCheckpoint;
            projectionPersistencePort.saveDelta(
                    domainProjector.flush(),
                    updatedCheckpoint);
        }
    }

    public STATE projection() {
        return cachedProjection;
    }
}
