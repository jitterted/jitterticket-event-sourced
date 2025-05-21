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

import java.util.ArrayList;
import java.util.Collection;
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
    public void save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        List<EventDto<EVENT>> existingEventDtos = idToEventDtoMap
                .computeIfAbsent(aggregateId, _ -> new ArrayList<>());
        List<EventDto<EVENT>> freshEventDtos =
                uncommittedEvents.map(event -> EventDto.from(
                                         aggregateId.id(),
                                         event.eventSequence(), // max(sequence) from existing events)
                                         event))
                                 .toList();
        existingEventDtos.addAll(freshEventDtos);

        idToEventDtoMap.put(aggregateId, existingEventDtos);
    }

    @Override
    protected List<EventDto<EVENT>> eventDtosFor(ID id) {
        return idToEventDtoMap.get(id);
    }

    @Override
    public Stream<EVENT> allEvents() {
        return idToEventDtoMap.values()
                              .stream()
                              .flatMap(Collection::stream)
                              .map(EventDto::toDomain);
    }
}
