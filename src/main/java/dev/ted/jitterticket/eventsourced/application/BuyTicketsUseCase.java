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
        // check if customer already has the max number of tickets for this concert
        // customer.canBuyTicketsFor(concertId, quantity)
        // ?? are use cases allowed to make decisions?
        Concert concert = concertStore.findById(concertId).orElseThrow();
        concert.buyTickets(customerId, quantity);
        // events: * TicketsBought(concertId, customerId, ...)
        //         * TicketsReceived(customerId, concertId, ticketOrderId??...)
        concertStore.save(concert);
        // return ticketOrderId inside of a Result object (success/failure)
        // later: notify customer of ticket purchase with a PDF and a link
    }
}
