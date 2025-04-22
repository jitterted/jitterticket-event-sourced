package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        void registerCustomerGeneratesCustomerRegistered() {
            CustomerId customerId = CustomerId.createRandom();

            Customer customer = Customer.register(
                    customerId, "customer name", "email@example.com");

            assertThat(customer.uncommittedEvents())
                    .containsExactly(
                            new CustomerRegistered(customerId,
                                                   "customer name",
                                                   "email@example.com")
                    );
        }

        @Test
        void purchaseTicketsGeneratesTicketsPurchased() {
            Customer customer = CustomerFactory.reconstituteWithRegisteredEvent();
            int quantity = 4;
            Concert concert = ConcertFactory.withTicketPriceOf(35);
            int paidAmount = quantity * 35;
            TicketOrderId ticketOrderId = TicketOrderId.createRandom();

            customer.purchaseTickets(concert, ticketOrderId, quantity);

            assertThat(customer.uncommittedEvents())
                    .containsExactly(
                            new TicketsPurchased(customer.getId(),
                                                 ticketOrderId,
                                                 concert.getId(),
                                                 quantity, paidAmount)
                    );
        }

    }

    @Nested
    class EventsProjectState {

        @Test
        void customerRegisteredUpdatesNameAndEmail() {
            CustomerId customerId = CustomerId.createRandom();
            CustomerRegistered customerRegistered = new CustomerRegistered(
                    customerId, "customer name", "email@example.com");

            Customer customer = Customer.reconstitute(List.of(customerRegistered));

            assertThat(customer.getId())
                    .isEqualTo(customerId);
            assertThat(customer.name())
                    .isEqualTo("customer name");
            assertThat(customer.email())
                    .isEqualTo("email@example.com");
        }

        @Test
        void ticketsPurchasedAddsTicketOrder() {
            CustomerId customerId = CustomerId.createRandom();
            CustomerRegistered customerRegistered = new CustomerRegistered(
                    customerId, "customer name", "email@example.com");
            ConcertId concertId = ConcertId.createRandom();
            int quantity = 8;
            int amountPaid = quantity * 45;
            TicketOrderId ticketOrderId = TicketOrderId.createRandom();
            TicketsPurchased ticketsPurchased = new TicketsPurchased(
                    customerId, ticketOrderId, concertId, quantity, amountPaid);

            Customer customer = Customer.reconstitute(List.of(customerRegistered,
                                                              ticketsPurchased));

            assertThat(customer.ticketOrders())
                    .containsExactly(
                            new Customer.TicketOrder(
                                    ticketOrderId, concertId, quantity, amountPaid)
                    );
        }
    }

}