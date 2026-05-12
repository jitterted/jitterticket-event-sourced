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
     * Does NOT tell subscribers that these events were saved: that happens in the above
     * save(AGGREGATE aggregate) method
     */
    Stream<EVENT> save(ID aggregateId, Stream<EVENT> uncommittedEvents);

    Optional<AGGREGATE> findById(ID id);

    List<EVENT> eventsForAggregate(ID id);

    /**
     * Subscribe and be notified of any new events that are saved to the event store.
     * This method defaults to sending *ALL* event types to consumers.
     * In order to only be informed about specific events, use
     * `subscribe(EventStreamConsumer consumer, Set desiredEvents)`
     *
     * @param eventStreamConsumer receives a Stream of events each time they are saved to the event store
     */
    @Deprecated // if you want all the events, you do the work to create the Set, otherwise use the subscribe below
    void subscribe(EventStreamConsumer eventStreamConsumer);

    void subscribe(EventStreamConsumer eventStreamConsumer,
                   Set<Class<? extends Event>> desiredEvents);

    @Deprecated // not sure anyone should ever do an unrestricted query like this
    Stream<EVENT> allEventsAfter(Checkpoint checkpoint);

    Stream<EVENT> allEventsAfter(Checkpoint checkpoint,
                                 Set<Class<? extends Event>> desiredEventTypes);
}
