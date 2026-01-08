package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.stream.Stream;

public class ProjectionCoordinator<EVENT extends Event, STATE>
        implements EventConsumer<EVENT> {

    private final DomainProjector<EVENT, STATE> domainProjector;
    private final EventStore<?, EVENT, ?> eventStore;
    private STATE cachedProjection;

    public ProjectionCoordinator(DomainProjector<EVENT, STATE> domainProjector,
                                 EventStore<?, EVENT, ?> eventStore) {
        this.domainProjector = domainProjector;
        this.eventStore = eventStore;
        eventStore.subscribe(this);
        cachedProjection = (STATE) new RegisteredCustomers();
        var projectorResult = domainProjector.project(cachedProjection,
                                                      eventStore.allEventsAfter(0L));
        cachedProjection = projectorResult.fullState();
    }

    @Override
    public void handle(Stream<EVENT> eventStream) {
        var result = domainProjector.project(cachedProjection,
                                             eventStream);
        cachedProjection = result.fullState();
    }

    public STATE projection() {
        return cachedProjection;
    }
}
