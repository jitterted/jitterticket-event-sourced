package dev.ted.jitterticket.eventsourced.application.port;

import dev.ted.jitterticket.eventsourced.application.Checkpoint;
import dev.ted.jitterticket.eventsourced.application.EventConsumer;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface EventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> {
    void save(AGGREGATE aggregate);

    Stream<EVENT> save(ID aggregateId, Stream<EVENT> uncommittedEvents);

    Optional<AGGREGATE> findById(ID id);

    List<EVENT> eventsForAggregate(ID id);

    void subscribe(EventConsumer<EVENT> eventConsumer);

    Stream<EVENT> allEventsAfter(Checkpoint checkpoint);
}
