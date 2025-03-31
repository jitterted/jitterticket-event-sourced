package dev.ted.jitterticket.eventsourced;

public sealed interface ConcertEvent permits ConcertRescheduled, ConcertScheduled {
}
