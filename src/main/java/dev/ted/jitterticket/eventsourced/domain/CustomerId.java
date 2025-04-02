package dev.ted.jitterticket.eventsourced.domain;

import java.util.UUID;

public record CustomerId(UUID id) implements Id {
}
