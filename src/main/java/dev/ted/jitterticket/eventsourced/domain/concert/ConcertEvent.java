package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.Event;

public abstract sealed class ConcertEvent extends Event
        permits
        ConcertRescheduled, ConcertScheduled,
        TicketsSold, TicketSalesStopped {

    protected final ConcertId concertId;

    protected ConcertEvent(ConcertId concertId, Long eventSequence) {
        super(eventSequence);
        this.concertId = concertId;
    }

    public ConcertId concertId() {
        return concertId;
    }
}
