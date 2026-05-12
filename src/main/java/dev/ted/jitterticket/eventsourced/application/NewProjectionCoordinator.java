package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Stream;

public class NewProjectionCoordinator<STATE, DELTA extends ProjectionDelta>
        implements EventStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(NewProjectionCoordinator.class);
    private final NewDomainProjector<STATE, DELTA> domainProjector;
    private final ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort;
    private Checkpoint lastWrittenCheckpoint;

    public NewProjectionCoordinator(
            NewDomainProjector<STATE, DELTA> domainProjector,
            ProjectionPersistencePort<STATE, DELTA> projectionPersistencePort,
            EventStore<?, ? extends Event, ?> eventStore) {
        this.projectionPersistencePort = projectionPersistencePort;

//        Checkpointed<STATE> snapshot = projectionPersistencePort.loadSnapshot();
//        STATE cachedProjection = snapshot.state();
//        lastWrittenCheckpoint = snapshot.checkpoint();

        this.domainProjector = projectionPersistencePort.loadProjector();
        this.lastWrittenCheckpoint = this.domainProjector.checkpoint();

        Set<Class<? extends Event>> handledEventTypes = this.domainProjector.handledEventTypes();

        log.info("Catching up on events for event types: {}", handledEventTypes);

        Stream<? extends Event> eventStream = eventStore.allEventsAfter(lastWrittenCheckpoint, handledEventTypes);
        log.info("Fetched events, now handling them...");
        handle(eventStream);

        log.info("Subscribing to event store for event types: {}", handledEventTypes);
        eventStore.subscribe(this, handledEventTypes);
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
