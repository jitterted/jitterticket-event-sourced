package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PurchaseTicketsUseCaseTest {

    @Test
    void failureOfPurchaseTicketsReturnsEmptyOptional() {
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
        Fixture fixture = createUseCase();

        Optional<TicketOrderId> ticketOrderId =
                fixture.purchaseTicketsUseCase.purchaseTickets(fixture.concertId, fixture.customer.getId(), 4);

        assertThat(ticketOrderId)
                .as("TicketOrderId should have been returned for a successful ticket purchase")
                .isPresent()
                .get()
                .isEqualTo(fixture.expectedTicketOrderUuid);
        Concert concertAfter = fixture.concertStore.findById(fixture.concertId).orElseThrow();
        assertThat(concertAfter.availableTicketCount())
                .isEqualTo(100 - 4);
        Customer customerAfter = fixture.customerStore.findById(fixture.customer.getId()).orElseThrow();
        assertThat(customerAfter.ticketOrders())
                .as("Customer should have 1 ticket order")
                .hasSize(1)
                .extracting(Customer.TicketOrder::concertId)
                .first()
                .isEqualTo(fixture.concertId);
    }

    private static Fixture createUseCase() {
        Customer customer = CustomerFactory.newlyRegistered();
        Concert concertBefore = ConcertFactory.createWithCapacity(100);
        TicketOrderId expectedTicketOrderUuid = new TicketOrderId(UUID.randomUUID());
        var concertStore = EventStore.forConcerts();
        concertStore.save(concertBefore);
        ConcertId concertId = concertBefore.getId();
        var customerStore = EventStore.forCustomers();
        customerStore.save(customer);
        PurchaseTicketsUseCase purchaseTicketsUseCase =
                PurchaseTicketsUseCase.createForTest(concertStore,
                                                     customerStore,
                                                     expectedTicketOrderUuid);
        return new Fixture(customer, expectedTicketOrderUuid, concertStore, concertId, customerStore, purchaseTicketsUseCase);
    }

    private record Fixture(Customer customer, TicketOrderId expectedTicketOrderUuid, EventStore<ConcertId,dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent, Concert> concertStore, ConcertId concertId, EventStore<dev.ted.jitterticket.eventsourced.domain.customer.CustomerId,dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent, Customer> customerStore, PurchaseTicketsUseCase purchaseTicketsUseCase) {}

}
