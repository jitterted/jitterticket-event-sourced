package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.util.Optional;
import java.util.UUID;

public class PurchaseTicketsUseCase {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public PurchaseTicketsUseCase(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    public Optional<TicketOrderId> buyTickets(ConcertId concertId, CustomerId customerId, int quantity) {
        // check if customer already has the max number of tickets for this concert
        // customer.canBuyTicketsFor(concertId, quantity)
        // ?? are use cases allowed to make decisions?
        return concertStore.findById(concertId)
                           .map(concert -> {
                               concert.buyTickets(customerId, quantity);
                               // events: * TicketsSold(concertId, ...)
                               //         * TicketsPurchased(customerId, concertId, ticketOrderId??...)
                               concertStore.save(concert);
                               return new TicketOrderId(UUID.randomUUID());
                           });

        // return ticketOrderId inside of a Result object (success/failure)
        // later: notify customer of ticket purchase with a PDF and a link
    }
}
