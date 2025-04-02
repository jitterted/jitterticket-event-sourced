package dev.ted.jitterticket.eventsourced.domain;

import java.util.UUID;

public record ConcertId(UUID id) implements Id {
}
