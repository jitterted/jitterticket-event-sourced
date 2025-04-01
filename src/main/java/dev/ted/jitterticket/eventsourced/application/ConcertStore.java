package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;

import java.util.stream.Stream;

public class ConcertStore {
    public Stream<Concert> findAll() {
        return Stream.empty();
    }
}
