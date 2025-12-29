package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class BaseEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>>
        implements EventStore<ID, EVENT, AGGREGATE> {

    protected final Function<List<EVENT>, AGGREGATE> eventsToAggregate;
    private ConcertSalesProjectionMediator eventConsumer;

    public BaseEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate) {
        this.eventsToAggregate = eventsToAggregate;
    }

    public void save(AGGREGATE aggregate) {
        ID aggregateId = aggregate.getId();
        if (aggregateId == null) {
            throw new IllegalArgumentException("The Aggregate " + aggregate + " must have an ID");
        }
        Stream<EVENT> uncommittedEvents = aggregate.uncommittedEvents();

        // we need the events that were saved so we have their event sequences
        Stream<EVENT> savedEvents = save(aggregateId, uncommittedEvents);
        if (eventConsumer != null) {
            eventConsumer.handle((Stream<ConcertEvent>) savedEvents);
        }
    }

    protected abstract @Nonnull List<EventDto<EVENT>> eventDtosFor(ID id);

    @Override
    public List<EVENT> eventsForAggregate(ID id) {
        return eventDtosFor(id)
                       .stream()
                       .map(EventDto::toDomain)
                       .toList();
    }

    @Override
    public void subscribe(ConcertSalesProjectionMediator eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public Optional<AGGREGATE> findById(ID id) {
        List<EVENT> events = eventsForAggregate(id);
        return events.isEmpty()
                ? Optional.empty()
                : Optional.of(eventsToAggregate.apply(events));
    }
}
