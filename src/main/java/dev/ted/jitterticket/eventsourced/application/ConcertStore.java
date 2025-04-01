package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ConcertStore {

    private final List<Concert> concerts = new ArrayList<>();

    public Stream<Concert> findAll() {
        return concerts.stream();
    }

    public void save(Concert concert) {
        concerts.add(concert);
    }
}
