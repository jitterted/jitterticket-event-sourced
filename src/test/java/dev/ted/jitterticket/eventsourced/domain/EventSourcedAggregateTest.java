package dev.ted.jitterticket.eventsourced.domain;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import org.junit.jupiter.api.Test;

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
    void eventSequenceNumbersAssignedUponBeingEnqueued() {
        Customer customer = Customer.register(CustomerId.createRandom(), "name", "email@example.com");
        customer.purchaseTickets(ConcertFactory.createConcert(), TicketOrderId.createRandom(), 3);
        customer.purchaseTickets(ConcertFactory.createConcert(), TicketOrderId.createRandom(), 1);

        assertThat(customer.uncommittedEvents())
                .extracting(Event::eventSequence)
                .containsExactly(0L, 1L, 2L);
    }


}
