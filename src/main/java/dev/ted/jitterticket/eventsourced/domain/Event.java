package dev.ted.jitterticket.eventsourced.domain;

public sealed interface Event permits ConcertEvent, CustomerEvent {
}
