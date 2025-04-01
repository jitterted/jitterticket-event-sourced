package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;

import java.util.stream.Stream;

public class BuyTicketsUseCase {

    public Stream<Concert> availableConcerts() {
        return Stream.empty();
    }

}
