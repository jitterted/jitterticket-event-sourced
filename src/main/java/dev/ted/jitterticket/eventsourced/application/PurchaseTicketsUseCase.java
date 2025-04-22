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
    private final TicketOrderIdGenerator ticketOrderIdGenerator;

    public PurchaseTicketsUseCase(EventStore<ConcertId, ConcertEvent, Concert> concertStore,
                                  EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        this.concertStore = concertStore;
        this.customerStore = customerStore;
        this.ticketOrderIdGenerator = () -> new TicketOrderId(UUID.randomUUID());
    }

    private PurchaseTicketsUseCase(EventStore<ConcertId, ConcertEvent, Concert> concertStore,
                                   EventStore<CustomerId, CustomerEvent, Customer> customerStore,
                                   TicketOrderId expectedTicketOrderUuid) {
        this.concertStore = concertStore;
        this.customerStore = customerStore;
        this.ticketOrderIdGenerator = () -> expectedTicketOrderUuid;
    }

    public static PurchaseTicketsUseCase createForTest(
            EventStore<ConcertId, ConcertEvent, Concert> concertStore,
            EventStore<CustomerId, CustomerEvent, Customer> customerStore,
            TicketOrderId expectedTicketOrderUuid) {
        return new PurchaseTicketsUseCase(concertStore, customerStore, expectedTicketOrderUuid);
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
                               TicketOrderId ticketOrderId = ticketOrderIdGenerator.nextTicketOrderId();
                               customer.purchaseTickets(concert, ticketOrderId, quantity);
                               customerStore.save(customer);
                               return ticketOrderId;
                           });

        // return ticketOrderId inside of a Result object (success/failure)
        // later: notify customer of ticket purchase with a PDF and a link
    }

    @FunctionalInterface
    interface TicketOrderIdGenerator {
        TicketOrderId nextTicketOrderId();
    }
}
