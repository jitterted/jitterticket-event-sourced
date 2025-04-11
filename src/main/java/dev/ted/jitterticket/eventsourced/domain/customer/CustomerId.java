package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.Id;

import java.util.UUID;

public record CustomerId(UUID id) implements Id {
    public static CustomerId createRandom() {
        return new CustomerId(UUID.randomUUID());
    }
}
