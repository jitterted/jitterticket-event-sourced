package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.CustomerId;

public class BuyTicketsUseCase {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public BuyTicketsUseCase(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public void buyTickets(ConcertId concertId, CustomerId customerId, int quantity) {
        Concert concert = concertStore.findById(concertId).orElseThrow();
        concert.buyTickets(customerId, quantity);
        concertStore.save(concert);
    }
}
