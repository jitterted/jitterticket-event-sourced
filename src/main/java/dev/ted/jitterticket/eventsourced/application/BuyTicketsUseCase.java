package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;

import java.util.stream.Stream;

public class BuyTicketsUseCase {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public BuyTicketsUseCase(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public Stream<Concert> availableConcerts() {
        return concertStore.findAll();
    }

}
