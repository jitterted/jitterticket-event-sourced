package dev.ted.jitterticket.eventsourced.domain;

public sealed interface Id
        permits ConcertId, CustomerId {
    Long id();
}
