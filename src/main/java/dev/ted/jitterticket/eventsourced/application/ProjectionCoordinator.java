package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;

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
        var projectorResult = domainProjector.project(
                snapshot.state(),
                eventStore.allEventsAfter(snapshot.checkpoint()));
        cachedProjection = projectorResult.fullState();
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
