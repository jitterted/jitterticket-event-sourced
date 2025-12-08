package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class InMemoryEventStore<
        ID extends Id,
        EVENT extends Event,
        AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> implements
        EventStore<ID, EVENT, AGGREGATE> {

    private final Map<ID, List<EventDto<EVENT>>> idToEventDtoMap = new HashMap<>();
    private long globalEventSequence = 0L; // emulate what the database does: starting at 1

    private InMemoryEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate) {
        super(eventsToAggregate);
    }

    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts() {
        return new InMemoryEventStore<>(Concert::reconstitute);
    }

    public static EventStore<CustomerId, CustomerEvent, Customer> forCustomers() {
        return new InMemoryEventStore<>(Customer::reconstitute);
    }

    @Override
    public long save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        List<EventDto<EVENT>> existingEventDtos = idToEventDtoMap
                .computeIfAbsent(aggregateId, _ -> new ArrayList<>());
        List<EventDto<EVENT>> freshEventDtos =
                uncommittedEvents.map(event -> EventDto.from(
                                         aggregateId.id(),
                                         event.eventSequence(),
                                         ++globalEventSequence,
                                         event))
                                 .toList();
        existingEventDtos.addAll(freshEventDtos);

        idToEventDtoMap.put(aggregateId, existingEventDtos);
        return globalEventSequence;
    }

    @Override
    protected @Nonnull List<EventDto<EVENT>> eventDtosFor(ID id) {
        return idToEventDtoMap.getOrDefault(id, List.of())
                .stream()
                .sorted(Comparator.comparingInt(EventDto::getEventSequence))
                .toList();
    }

    @Override
    public Stream<EVENT> allEvents() {
        return allEventsSortedByGlobalEventSequence()
                .map(EventDto::toDomain);
    }

    @Override
    public Stream<EVENT> allEventsAfter(long globalEventSequence) {
        return allEventsSortedByGlobalEventSequence()
                              .dropWhile(eventDto -> eventDto.getGlobalEventSequence() <= globalEventSequence)
                              .map(EventDto::toDomain);
    }

    private Stream<EventDto<EVENT>> allEventsSortedByGlobalEventSequence() {
        return idToEventDtoMap.values()
                              .stream()
                              .flatMap(Collection::stream)
                              .sorted(Comparator.comparingLong(EventDto::getGlobalEventSequence));
    }
}
