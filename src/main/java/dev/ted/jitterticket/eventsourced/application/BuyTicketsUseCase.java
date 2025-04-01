package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;

import java.util.stream.Stream;

public class BuyTicketsUseCase {

    private final ConcertStore concertStore;

    public BuyTicketsUseCase(ConcertStore concertStore) {
        this.concertStore = concertStore;
    }

    public Stream<Concert> availableConcerts() {
        return Stream.empty();
    }

}
