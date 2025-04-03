package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class ConcertStore<
        ID extends Id,
        EVENT extends Event,
        AGGREGATE extends EventSourcedAggregate<EVENT, ID>> {

    private final Map<ID, List<EventDto<EVENT>>> idToEventDtoMap = new HashMap<>();

    @Deprecated // use findById() instead
    public Stream<AGGREGATE> findAll() {
        return idToEventDtoMap
                .keySet()
                .stream()
                .map(id -> concertFromEvents(idToEventDtoMap.get(id)));
    }

    public void save(AGGREGATE concert) {
        if (concert.getId() == null) {
            throw new IllegalArgumentException("concert must have an ID");
        }
        List<EventDto<EVENT>> existingEventDtos = idToEventDtoMap
                .computeIfAbsent(concert.getId(),
                                 _ -> new ArrayList<>());
        List<EventDto<EVENT>> freshEventDtos = concert.uncommittedEvents()
                                                      .stream()
                                                      .map(event -> EventDto.from(
                                                       concert.getId().id(),
                                                       0,
                                                       event))
                                                      .toList();
        existingEventDtos.addAll(freshEventDtos);

        idToEventDtoMap.put(concert.getId(), existingEventDtos);
    }

    private AGGREGATE concertFromEvents(List<EventDto<EVENT>> existingEventDtos) {
        List<EVENT> events = existingEventDtos
                .stream()
                .map(existingEventDto -> (EVENT) existingEventDto.toDomain())
                .toList();
        return (AGGREGATE) Concert.reconstitute((List<ConcertEvent>) events);
    }

    public Optional<AGGREGATE> findById(ID id) {
        return Optional.ofNullable(idToEventDtoMap.get(id))
                       .map(this::concertFromEvents);
    }
}
