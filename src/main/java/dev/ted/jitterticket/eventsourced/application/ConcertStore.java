package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertStore {

    private final List<Concert> concerts = new ArrayList<>();
    private final Map<Id, List<EventDto>> idToEventDtoMap = new HashMap<>();

    public Stream<Concert> findAll() {
        return concerts.stream();
    }

    public void save(Concert concert) {
        if (concert.getId() == null) {
            throw new IllegalArgumentException("concert must have an ID");
        }
        List<EventDto> existingEventDtos = idToEventDtoMap
                .computeIfAbsent(concert.getId(),
                                 _ -> new ArrayList<>());
        concerts.add(concert);
    }
}
