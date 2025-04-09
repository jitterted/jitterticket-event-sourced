package dev.ted.jitterticket.eventsourced.domain;

import java.util.UUID;

public record ConcertId(UUID id) implements Id {
    public static ConcertId createRandom() {
        return new ConcertId(UUID.randomUUID());
    }
}
