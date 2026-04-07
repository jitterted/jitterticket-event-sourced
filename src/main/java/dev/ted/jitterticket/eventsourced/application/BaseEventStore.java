package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class BaseEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>>
        implements EventStore<ID, EVENT, AGGREGATE> {

    protected final Function<List<EVENT>, AGGREGATE> eventsToAggregate;
    private final List<EventStreamConsumer> eventStreamConsumers = new ArrayList<>();
    private final Map<EventStreamConsumer, Set<Class<? extends Event>>> consumersToDesiredEvents = new HashMap<>();

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
        eventStreamConsumers.forEach(
                eventConsumer -> {
                    Set<Class<? extends Event>> desiredEventClasses = consumersToDesiredEvents.get(eventConsumer);
                    if (desiredEventClasses == null) {
                        throw new IllegalStateException("No desired events defined for " + eventConsumer);
                    }
                    Predicate<EVENT> eventMatchingPredicate =
                            event -> desiredEventClasses.contains(event.getClass());
                    List<? extends Event> desiredEvents =
                            savedEvents.stream()
                                       .filter(eventMatchingPredicate)
                                       .toList();
                    if (!desiredEvents.isEmpty()) {
                        eventConsumer.handle(desiredEvents.stream());
                    }
                });
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
    public void subscribe(EventStreamConsumer eventStreamConsumer) {
        eventStreamConsumers.add(eventStreamConsumer);
        consumersToDesiredEvents.put(eventStreamConsumer,
                                     Set.of(
                                             CustomerRegistered.class,
                                             TicketsPurchased.class,
                                             ConcertRescheduled.class,
                                             ConcertScheduled.class,
                                             TicketsSold.class,
                                             TicketSalesStopped.class));
    }

    @Override
    public void subscribe(EventStreamConsumer eventStreamConsumer, Set<Class<? extends Event>> desiredEvents) {
        eventStreamConsumers.add(eventStreamConsumer);
        consumersToDesiredEvents.put(eventStreamConsumer, desiredEvents);
    }

    @Override
    public Optional<AGGREGATE> findById(ID id) {
        List<EVENT> events = eventsForAggregate(id);
        return events.isEmpty()
                ? Optional.empty()
                : Optional.of(eventsToAggregate.apply(events));
    }
}
