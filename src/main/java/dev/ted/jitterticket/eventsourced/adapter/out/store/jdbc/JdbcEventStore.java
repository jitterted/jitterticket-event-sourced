package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.BaseEventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class JdbcEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> {
    public JdbcEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate) {
        super(eventsToAggregate);
    }

    @Override
    protected List<EventDto<EVENT>> eventDtosFor(ID id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<EVENT> allEvents() {
        throw new UnsupportedOperationException();
    }
}
