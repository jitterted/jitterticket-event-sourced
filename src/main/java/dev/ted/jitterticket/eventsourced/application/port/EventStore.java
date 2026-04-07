package dev.ted.jitterticket.eventsourced.application.port;

import dev.ted.jitterticket.eventsourced.application.Checkpoint;
import dev.ted.jitterticket.eventsourced.application.EventStreamConsumer;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

public interface EventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> {
    void save(AGGREGATE aggregate);

    /**
     * Saves events associated with the aggregate ID so they can be retrieved later.
     * Does NOT tell subscribers that these events were saved, that happens in the above
     * save(AGGREGATE aggregate) method
     */
    Stream<EVENT> save(ID aggregateId, Stream<EVENT> uncommittedEvents);

    Optional<AGGREGATE> findById(ID id);

    List<EVENT> eventsForAggregate(ID id);

    @Deprecated // if you want all the events, you do the work to create the Set, otherwise use the subscribe below
    void subscribe(EventStreamConsumer eventStreamConsumer);

    void subscribe(EventStreamConsumer eventStreamConsumer,
                   Set<Class<? extends Event>> desiredEvents);

    @Deprecated // not sure anyone should ever do an unrestricted query like this
    Stream<EVENT> allEventsAfter(Checkpoint checkpoint);

    Stream<EVENT> allEventsAfter(Checkpoint checkpoint,
                                 Set<Class<? extends Event>> desiredEventTypes);
}
