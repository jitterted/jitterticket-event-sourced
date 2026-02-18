package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.UUID;

public record ConcertId(UUID id) implements Id {
    public static ConcertId createRandom() {
        return new ConcertId(UUID.randomUUID());
    }

    public static ConcertId from(String uuid) {
        return new ConcertId(UUID.fromString(uuid));
    }
}
