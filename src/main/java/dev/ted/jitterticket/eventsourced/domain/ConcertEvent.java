package dev.ted.jitterticket.eventsourced.domain;

public sealed interface ConcertEvent permits ConcertRescheduled, ConcertScheduled {
}
