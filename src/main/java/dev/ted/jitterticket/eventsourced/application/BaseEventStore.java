package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class BaseEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>>
        implements EventStore<ID, EVENT, AGGREGATE> {

    protected final Function<List<EVENT>, AGGREGATE> eventsToAggregate;
    private ConcertSalesProjector concertSalesProjector;

    public BaseEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate) {
        this.eventsToAggregate = eventsToAggregate;
    }

    public void save(AGGREGATE aggregate) {
        ID aggregateId = aggregate.getId();
        if (aggregateId == null) {
            throw new IllegalArgumentException("The Aggregate " + aggregate + " must have an ID");
        }
        Stream<EVENT> uncommittedEvents = aggregate.uncommittedEvents();

        save(aggregateId, uncommittedEvents);
        // notify projectors to update their state, e.g.:
        if (concertSalesProjector != null) {
            concertSalesProjector.apply((Stream<ConcertEvent>) aggregate.uncommittedEvents());
        }
    }

    protected abstract List<EventDto<EVENT>> eventDtosFor(ID id);

    @Override
    public List<EVENT> eventsForAggregate(ID id) {
        return Optional.ofNullable(eventDtosFor(id))
                       .stream()
                       .flatMap(Collection::stream)
                       .map(EventDto::toDomain)
                       .toList();
    }

    @Override
    public void register(ConcertSalesProjector concertSalesProjector) {
        this.concertSalesProjector = concertSalesProjector;
        concertSalesProjector.apply((Stream<ConcertEvent>) allEvents());
    }

    @Override
    public Optional<AGGREGATE> findById(ID id) {
        List<EVENT> events = eventsForAggregate(id);
        return events.isEmpty()
                ? Optional.empty()
                : Optional.of(eventsToAggregate.apply(events));
    }
}
