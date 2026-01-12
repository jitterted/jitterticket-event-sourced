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

    public ProjectionCoordinator(DomainProjector<EVENT, STATE> domainProjector,
                                 ProjectionPersistencePort<STATE> projectionPersistencePort,
                                 EventStore<?, EVENT, ?> eventStore) {
        this.domainProjector = domainProjector;
        this.projectionPersistencePort = projectionPersistencePort;
        this.eventStore = eventStore;
        eventStore.subscribe(this);
        var snapshot = projectionPersistencePort.loadSnapshot();
        AtomicReference<Long> lastEventSeen = new AtomicReference<>(snapshot.checkpoint().value());
        Stream<EVENT> eventStream = eventStore
                .allEventsAfter(snapshot.checkpoint())
                .peek(event -> lastEventSeen.set(event.eventSequence()));
        var projectorResult = domainProjector.project(
                snapshot.state(),
                eventStream);
        cachedProjection = projectorResult.fullState();
        projectionPersistencePort.saveDelta(
                projectorResult.delta(),
                lastEventSeen.get() == 0L
                        ? Checkpoint.INITIAL
                        : Checkpoint.of(lastEventSeen.get()));
    }

    @Override
    public void handle(Stream<EVENT> eventStream) {
        var result = domainProjector.project(cachedProjection,
                                             eventStream);
        cachedProjection = result.fullState();
        // persist the latest cache
    }

    public STATE projection() {
        return cachedProjection;
    }
}
