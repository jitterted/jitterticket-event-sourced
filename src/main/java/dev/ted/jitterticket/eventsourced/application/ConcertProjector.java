package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;

import java.util.stream.Stream;

public class ConcertProjector {

    public ConcertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {

    }

    public Stream<ConcertId> allConcerts() {
        return Stream.empty();
    }

}
