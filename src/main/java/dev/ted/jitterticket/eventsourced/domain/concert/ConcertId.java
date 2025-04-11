package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.UUID;

public record ConcertId(UUID id) implements Id {
    public static ConcertId createRandom() {
        return new ConcertId(UUID.randomUUID());
    }
}
