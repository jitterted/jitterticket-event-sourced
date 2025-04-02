package dev.ted.jitterticket.eventsourced.domain;

import java.util.UUID;

public sealed interface Id
        permits ConcertId, CustomerId {
    UUID id();
}
