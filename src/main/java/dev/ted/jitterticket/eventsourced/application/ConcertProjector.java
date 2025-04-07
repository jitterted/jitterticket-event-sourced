package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.ConcertScheduled;

import java.util.stream.Stream;

public class ConcertProjector {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public ConcertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public Stream<ConcertId> allConcerts() {
        return concertStore.allEvents()
                           .filter(concertEvent -> concertEvent instanceof ConcertScheduled)
                           .map(concertEvent -> ((ConcertScheduled) concertEvent).concertId());
    }

    public Stream<ConcertTicketView> allConcertTicketViews() {
        return concertStore.allEvents()
                           .filter(concertEvent -> concertEvent instanceof ConcertScheduled)
                           .map(concertEvent -> (ConcertScheduled) concertEvent)
                           .map(concertScheduled ->
                                        new ConcertTicketView(
                                                concertScheduled.concertId(),
                                                concertScheduled.artist(),
                                                concertScheduled.ticketPrice(),
                                                concertScheduled.showDateTime(),
                                                concertScheduled.doorsTime()
                                        ));
    }
}

