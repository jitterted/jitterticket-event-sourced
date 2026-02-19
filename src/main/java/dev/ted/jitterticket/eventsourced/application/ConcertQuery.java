package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

public class ConcertQuery {
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    public ConcertQuery(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.concertEventStore = concertEventStore;
    }

    public Concert find(ConcertId id) {
        return concertEventStore
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Could not find concert with id: " + id.id()));
    }
}
