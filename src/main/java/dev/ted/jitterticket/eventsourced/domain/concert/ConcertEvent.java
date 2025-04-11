package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.Event;

public sealed interface ConcertEvent extends Event
        permits ConcertRescheduled, ConcertScheduled, TicketsBought {
    ConcertId concertId();
}
