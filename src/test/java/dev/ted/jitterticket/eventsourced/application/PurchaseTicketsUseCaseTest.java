package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class PurchaseTicketsUseCaseTest {

    @Test
    void failureOfBuyTicketsReturnsEmptyOptional() {
        var concertStore = EventStore.forConcerts();
        var customerStore = EventStore.forCustomers();
        Customer customer = CustomerFactory.newlyRegistered();
        customerStore.save(customer);
        PurchaseTicketsUseCase purchaseTicketsUseCase = new PurchaseTicketsUseCase(concertStore, customerStore);
        ConcertId invalidConcertId = ConcertId.createRandom();

        Optional<TicketOrderId> ticketOrderIdOptional =
                purchaseTicketsUseCase.purchaseTickets(invalidConcertId,
                                                       customer.getId(), 1);

        assertThat(ticketOrderIdOptional)
                .as("Ticket order should have failed and therefore returned an empty TicketOrderId")
                .isEmpty();
    }

    @Test
    void purchaseTicketsReducesNumberOfConcertTicketsAvailableAndAddsTicketOrderToCustomer() {
        var concertStore = EventStore.forConcerts();
        Concert concertBefore = ConcertFactory.createWithCapacity(100);
        concertStore.save(concertBefore);
        ConcertId concertId = concertBefore.getId();
        var customerStore = EventStore.forCustomers();
        Customer customer = CustomerFactory.newlyRegistered();
        customerStore.save(customer);

        PurchaseTicketsUseCase purchaseTicketsUseCase =
                new PurchaseTicketsUseCase(concertStore, customerStore);

        Optional<TicketOrderId> ticketOrderId =
                purchaseTicketsUseCase.purchaseTickets(concertId, customer.getId(), 4);

        assertThat(ticketOrderId)
                .as("TicketOrderId should have been returned for a successful ticket purchase")
                .isPresent();
        Concert concertAfter = concertStore.findById(concertId).orElseThrow();
        assertThat(concertAfter.availableTicketCount())
                .isEqualTo(100 - 4);
        Customer customerAfter = customerStore.findById(customer.getId()).orElseThrow();
        assertThat(customerAfter.ticketOrders())
                .as("Customer should have 1 ticket order")
                .hasSize(1)
                .extracting(Customer.TicketOrder::concertId)
                .first()
                .isEqualTo(concertId);
    }

}
