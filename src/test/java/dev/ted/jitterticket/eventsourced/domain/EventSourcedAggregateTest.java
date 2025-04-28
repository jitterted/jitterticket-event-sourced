package dev.ted.jitterticket.eventsourced.domain;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class EventSourcedAggregateTest {

    @Test
    void eventsAreAppliedUponBeingEnqueued() {
        var eventSourcedAggregate = new EventSourcedAggregate<>() {
            private Event appliedEvent;

            @Override
            protected void apply(Event event) {
                appliedEvent = event;
            }
        };

        CustomerRegistered event = new CustomerRegistered(CustomerId.createRandom(), 0L, "name", "email");
        eventSourcedAggregate.enqueue(event);

        assertThat(eventSourcedAggregate.appliedEvent)
                .isEqualTo(event);
    }

    @Test
    void eventSequenceNumbersAssignedUponCreation() {
        Customer customer = Customer.register(CustomerId.createRandom(), "name", "email@example.com");
        customer.purchaseTickets(ConcertFactory.createConcert(), TicketOrderId.createRandom(), 3);
        customer.purchaseTickets(ConcertFactory.createConcert(), TicketOrderId.createRandom(), 1);

        assertThat(customer.uncommittedEvents())
                .extracting(Event::eventSequence)
                .containsExactly(0L, 1L, 2L);
    }

    @Test
    void eventSequenceAssignmentTakesIntoAccountEventsLoadedDuringReconstitution() {
        CustomerId customerId = CustomerId.createRandom();
        CustomerRegistered customerRegistered = new CustomerRegistered(
                customerId, 0L, "name", "email@example.com");
        TicketsPurchased ticketsPurchased = new TicketsPurchased(
                customerId, 1L, TicketOrderId.createRandom(), ConcertId.createRandom(), 3, 150);
        Customer reconstitutedCustomer = Customer.reconstitute(List.of(customerRegistered,
                                                                       ticketsPurchased));

        reconstitutedCustomer.purchaseTickets(ConcertFactory.createConcert(), TicketOrderId.createRandom(), 3);

        assertThat(reconstitutedCustomer.uncommittedEvents())
                .extracting(Event::eventSequence)
                .containsExactly(2L);
    }
}
