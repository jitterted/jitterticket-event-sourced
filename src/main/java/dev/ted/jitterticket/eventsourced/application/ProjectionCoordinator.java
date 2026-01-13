package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

public class ProjectionCoordinator<EVENT extends Event, STATE>
        implements EventConsumer<EVENT> {

    private final DomainProjector<EVENT, STATE> domainProjector;
    private final ProjectionPersistencePort<STATE> projectionPersistencePort;
    private final EventStore<?, EVENT, ?> eventStore;
    private STATE cachedProjection;
    private Checkpoint cachedCheckpoint;

    public ProjectionCoordinator(DomainProjector<EVENT, STATE> domainProjector,
                                 ProjectionPersistencePort<STATE> projectionPersistencePort,
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
    public void handle(Stream<EVENT> eventStream) {
        updateProjection(eventStream);
    }

    private void updateProjection(Stream<EVENT> eventStream) {
        AtomicReference<Long> lastEventSeen =
                new AtomicReference<>(cachedCheckpoint.value());
        Stream<EVENT> checkpointTrackingEventStream = eventStream
                .peek(event -> lastEventSeen.set(event.eventSequence()));

        var projectorResult = domainProjector.project(
                cachedProjection,
                checkpointTrackingEventStream);

        cachedProjection = projectorResult.fullState();

        cachedCheckpoint = lastEventSeen.get() == 0L
                ? Checkpoint.INITIAL
                : Checkpoint.of(lastEventSeen.get());

        projectionPersistencePort.saveDelta(
                projectorResult.delta(),
                cachedCheckpoint);
    }

    public STATE projection() {
        return cachedProjection;
    }
}
