package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PurchaseTicketsUseCaseTest {

    @Test
    void failureOfPurchaseTicketsReturnsEmptyOptional() {
        var concertStore = InMemoryEventStore.forConcerts();
        var customerStore = InMemoryEventStore.forCustomers();
        Customer customer = CustomerFactory.newlyRegistered();
        customerStore.save(customer);
        PurchaseTicketsUseCase purchaseTicketsUseCase =
                new PurchaseTicketsUseCase(concertStore, customerStore);
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
        int originalConcertAvailableTicketCount = 100;
        int ticketPurchaseQuantity = 4;
        TicketOrderId expectedTicketOrderId = new TicketOrderId(UUID.randomUUID());
        Fixture fixture = createForPurchaseTicketsWithCapacityOf(originalConcertAvailableTicketCount);
        PurchaseTicketsUseCase purchaseTicketsUseCase =
                PurchaseTicketsUseCase.createForTest(fixture.concertStore(),
                                                     fixture.customerStore(),
                                                     expectedTicketOrderId);

        Optional<TicketOrderId> actualTicketOrderId =
                purchaseTicketsUseCase.purchaseTickets(fixture.concertId(),
                        fixture.customer().getId(), ticketPurchaseQuantity);

        assertThat(actualTicketOrderId)
                .as("TicketOrderId should have been returned for a successful ticket purchase")
                .isPresent()
                .get()
                .isEqualTo(expectedTicketOrderId);
        Concert concertAfter = fixture.concertStore().findById(fixture.concertId()).orElseThrow();
        assertThat(concertAfter.availableTicketCount())
                .isEqualTo(originalConcertAvailableTicketCount - ticketPurchaseQuantity);
        Customer customerAfter = fixture.customerStore().findById(fixture.customer().getId()).orElseThrow();
        assertThat(customerAfter.ticketOrders())
                .as("Customer should have 1 ticket order")
                .hasSize(1)
                .extracting(Customer.TicketOrder::ticketOrderId, Customer.TicketOrder::concertId)
                .first()
                .isEqualTo(tuple(expectedTicketOrderId, fixture.concertId()));
    }

    @Test
    void ticketOrderIdsAreUniquePerPurchase() {
        Fixture fixture = createForPurchaseTickets();
        PurchaseTicketsUseCase purchaseTicketsUseCase =
                new PurchaseTicketsUseCase(fixture.concertStore,
                                           fixture.customerStore);

        Optional<TicketOrderId> firstTicketOrderId =
                purchaseTicketsUseCase.purchaseTickets(fixture.concertId, fixture.customer.getId(), 2);
        Optional<TicketOrderId> secondTicketOrderId =
                purchaseTicketsUseCase.purchaseTickets(fixture.concertId, fixture.customer.getId(), 2);

        assertThat(firstTicketOrderId)
                .as("Ticket Order IDs should be different")
                .isNotEqualTo(secondTicketOrderId);
    }

    //region Test Fixture

    private static Fixture createForPurchaseTicketsWithCapacityOf(int originalConcertAvailableTicketCount) {
        Customer customer = CustomerFactory.newlyRegistered();
        Concert concertBefore = ConcertFactory.createWithCapacity(originalConcertAvailableTicketCount);
        var concertStore = InMemoryEventStore.forConcerts();
        concertStore.save(concertBefore);
        ConcertId concertId = concertBefore.getId();
        var customerStore = InMemoryEventStore.forCustomers();
        customerStore.save(customer);
        return new Fixture(customer, concertStore, concertId, customerStore);
    }

    private record Fixture(Customer customer, EventStore<ConcertId, ConcertEvent, Concert> concertStore, ConcertId concertId, EventStore<dev.ted.jitterticket.eventsourced.domain.customer.CustomerId,dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent, Customer> customerStore) {}

    private static Fixture createForPurchaseTickets() {
        return createForPurchaseTicketsWithCapacityOf(42);
    }

    //endregion Test Fixture

}
