package dev.ted.jitterticket.eventsourced.domain;

import java.util.UUID;

public record CustomerId(UUID id) implements Id {
    public static CustomerId createRandom() {
        return new CustomerId(UUID.randomUUID());
    }
}
