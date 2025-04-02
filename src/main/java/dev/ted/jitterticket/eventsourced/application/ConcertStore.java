package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

// will become Store<Concert>
public class ConcertStore {

    private final Map<Id, List<EventDto<ConcertEvent>>> idToEventDtoMap = new HashMap<>();

    public Stream<Concert> findAll() {
        return idToEventDtoMap
                .keySet()
                .stream()
                .map(id -> concertFromEvents(idToEventDtoMap.get(id)));
    }

    public void save(Concert concert) {
        if (concert.getId() == null) {
            throw new IllegalArgumentException("concert must have an ID");
        }
        List<EventDto<ConcertEvent>> existingEventDtos = idToEventDtoMap
                .computeIfAbsent(concert.getId(),
                                 _ -> new ArrayList<>());
        List<EventDto<ConcertEvent>> freshEventDtos = concert.uncommittedEvents()
                                               .stream()
                                               .map(event -> EventDto.from(
                                                       concert.getId().id(),
                                                       0,
                                                       event))
                                               .toList();
        existingEventDtos.addAll(freshEventDtos);

        idToEventDtoMap.put(concert.getId(), existingEventDtos);
    }

    private Concert concertFromEvents(List<EventDto<ConcertEvent>> existingEventDtos) {
        List<ConcertEvent> events = existingEventDtos
                .stream()
                .map(EventDto::toDomain)
                .toList();
        return Concert.reconstitute(events);
    }
}
