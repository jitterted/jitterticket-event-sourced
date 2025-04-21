package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PurchaseTicketsUseCaseTest {

    @Test
    void failureOfBuyTicketsReturnsEmptyOptional() {
        var concertStore = EventStore.forConcerts();
        PurchaseTicketsUseCase purchaseTicketsUseCase = new PurchaseTicketsUseCase(concertStore, EventStore.forCustomers());
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        ConcertId invalidConcertId = ConcertId.createRandom();

        Optional<TicketOrderId> ticketOrderIdOptional =
                purchaseTicketsUseCase.purchaseTickets(invalidConcertId,
                                                       customerId, 1);

        assertThat(ticketOrderIdOptional)
                .as("Ticket order should have failed and therefore returned an empty TicketOrderId")
                .isEmpty();
    }

    @Test
    @Disabled("dev.ted.jitterticket.eventsourced.application.PurchaseTicketsUseCaseTest 4/21/25 13:09 — until Customer fully supports tracking Ticket Orders")
    void purchaseTicketsReducesNumberOfConcertTicketsAvailableAndAddsTicketOrderToCustomer() {
        var concertStore = EventStore.forConcerts();
        Concert concertBefore = ConcertFactory.createWithCapacity(100);
        concertStore.save(concertBefore);
        ConcertId concertId = concertBefore.getId();
        var customerStore = EventStore.forCustomers();
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        Customer customer = Customer.register(customerId, "Cust Omer", "customer@example.com");
        customerStore.save(customer);

        PurchaseTicketsUseCase purchaseTicketsUseCase =
                new PurchaseTicketsUseCase(concertStore, customerStore);

        Optional<TicketOrderId> ticketOrderId =
                purchaseTicketsUseCase.purchaseTickets(concertId, customerId, 4);

        assertThat(ticketOrderId)
                .as("TicketOrderId should have been returned for a successful ticket purchase")
                .isPresent();
        Concert concertAfter = concertStore.findById(concertId).orElseThrow();
        assertThat(concertAfter.availableTicketCount())
                .isEqualTo(100 - 4);
        Customer customerAfter = customerStore.findById(customerId).orElseThrow();
        assertThat(customerAfter.ticketOrders())
                .as("Customer should have 1 ticket order")
                .hasSize(1)
                .extracting(Customer.TicketOrder::concertId)
                .first()
                .isEqualTo(concertId);
    }

}
