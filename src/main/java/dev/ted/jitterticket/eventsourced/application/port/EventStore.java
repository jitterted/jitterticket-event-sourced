package dev.ted.jitterticket.eventsourced.application.port;

import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjector;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public interface EventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> {
    void save(AGGREGATE aggregate);

    void save(ID aggregateId, Stream<EVENT> uncommittedEvents);

    Optional<AGGREGATE> findById(ID id);

    Stream<EVENT> allEvents();

    List<EVENT> eventsForAggregate(ID id);

    void subscribe(ConcertSalesProjector concertSalesProjector, long lastGlobalEventSequence);

    Stream<EVENT> allEventsAfter(long globalEventSequence);
}
