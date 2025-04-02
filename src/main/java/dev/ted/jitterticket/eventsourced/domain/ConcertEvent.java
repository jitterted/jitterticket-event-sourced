package dev.ted.jitterticket.eventsourced.domain;

public sealed interface ConcertEvent extends Event
        permits ConcertRescheduled, ConcertScheduled {
}
