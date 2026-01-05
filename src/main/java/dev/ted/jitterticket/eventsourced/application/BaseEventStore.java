package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class BaseEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>>
        implements EventStore<ID, EVENT, AGGREGATE> {

    protected final Function<List<EVENT>, AGGREGATE> eventsToAggregate;
    private final List<EventConsumer<EVENT>> eventConsumers = new ArrayList<>();

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
        // convert to a list so we can pass a stream to each event consumer without worrying about being consumed
        List<EVENT> savedEvents = save(aggregateId, uncommittedEvents).toList();
        eventConsumers.forEach(eventConsumer ->
                                       eventConsumer.handle(savedEvents.stream()));
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
    public void subscribe(EventConsumer<EVENT> eventConsumer) {
        eventConsumers.add(eventConsumer);
    }

    @Override
    public Optional<AGGREGATE> findById(ID id) {
        List<EVENT> events = eventsForAggregate(id);
        return events.isEmpty()
                ? Optional.empty()
                : Optional.of(eventsToAggregate.apply(events));
    }
}
