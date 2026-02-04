package dev.ted.jitterticket.eventsourced.application;

import java.util.List;

public record AvailableConcerts(List<AvailableConcert> availableConcerts) {
    static final AvailableConcerts EMPTY = new AvailableConcerts(List.of());
}
