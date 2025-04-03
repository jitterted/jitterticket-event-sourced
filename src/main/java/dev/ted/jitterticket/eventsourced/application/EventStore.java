package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.Customer;
import dev.ted.jitterticket.eventsourced.domain.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class EventStore<
        ID extends Id,
        EVENT extends Event,
        AGGREGATE extends EventSourcedAggregate<EVENT, ID>> {

    private final Map<ID, List<EventDto<EVENT>>> idToEventDtoMap = new HashMap<>();
    private final Function<List<EVENT>, AGGREGATE> eventsToAggregate;

    private EventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate) {
        this.eventsToAggregate = eventsToAggregate;
    }

    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts() {
        return new EventStore<>(Concert::reconstitute);
    }

    public static EventStore<CustomerId, CustomerEvent, Customer> forCustomers() {
        return new EventStore<>(Customer::reconstitute);
    }

    @Deprecated // use findById() instead
    public Stream<AGGREGATE> findAll() {
        return idToEventDtoMap
                .keySet()
                .stream()
                .map(id -> concertFromEvents(idToEventDtoMap.get(id)));
    }

    public void save(AGGREGATE aggregate) {
        if (aggregate.getId() == null) {
            throw new IllegalArgumentException("The Aggregate " + aggregate + " must have an ID");
        }
        List<EventDto<EVENT>> existingEventDtos = idToEventDtoMap
                .computeIfAbsent(aggregate.getId(),
                                 _ -> new ArrayList<>());
        List<EventDto<EVENT>> freshEventDtos = aggregate.uncommittedEvents()
                                                        .stream()
                                                        .map(event -> EventDto.from(
                                                                aggregate.getId().id(),
                                                                0,
                                                                event))
                                                        .toList();
        existingEventDtos.addAll(freshEventDtos);

        idToEventDtoMap.put(aggregate.getId(), existingEventDtos);
    }

    private AGGREGATE concertFromEvents(List<EventDto<EVENT>> existingEventDtos) {
        List<EVENT> events = existingEventDtos
                .stream()
                .map(EventDto::toDomain)
                .toList();
        return eventsToAggregate.apply(events);
    }

    public Optional<AGGREGATE> findById(ID id) {
        return Optional.ofNullable(idToEventDtoMap.get(id))
                       .map(this::concertFromEvents);
    }

    public Stream<EVENT> allEvents() {
        return idToEventDtoMap.values()
                              .stream()
                              .flatMap(Collection::stream)
                              .map(EventDto::toDomain);
    }
}
