package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.util.Optional;
import java.util.UUID;

public class PurchaseTicketsUseCase {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;
    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;

    public PurchaseTicketsUseCase(EventStore<ConcertId, ConcertEvent, Concert> concertStore,
                                  EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        this.concertStore = concertStore;
        this.customerStore = customerStore;
    }

    public Optional<TicketOrderId> purchaseTickets(ConcertId concertId, CustomerId customerId, int quantity) {
        // check if customer already has the max number of tickets for this concert
        // customer.canPurchaseTicketsFor(concertId, quantity)
        // ?? are use cases allowed to make decisions?
        Customer customer = customerStore.findById(customerId)
                                         .orElseThrow(() -> new RuntimeException("Customer not found for ID: " + customerId));
        return concertStore.findById(concertId)
                           .map(concert -> {
                               concert.sellTicketsTo(customerId, quantity);
                               concertStore.save(concert);
                               customer.purchaseTickets(concert, quantity);
                               customerStore.save(customer);
                               return nextTicketOrderId();
                           });

        // return ticketOrderId inside of a Result object (success/failure)
        // later: notify customer of ticket purchase with a PDF and a link
    }

    private static TicketOrderId nextTicketOrderId() {
        return new TicketOrderId(UUID.randomUUID());
    }
}
