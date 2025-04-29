package dev.ted.jitterticket.eventsourced.domain;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
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
    void eventSequenceNumbersAssignedUponCommandExecutionForConcert() {
        Concert concert = ConcertFactory.createConcert();
        concert.rescheduleTo(concert.showDateTime().plusMonths(1), concert.doorsTime());
        concert.sellTicketsTo(CustomerId.createRandom(), 4);

        assertThat(concert.uncommittedEvents())
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

    @Test
    void eventSequenceAssignmentTakesIntoAccountEventsLoadedDuringReconstitutionForConcert() {
        ConcertId concertId = ConcertId.createRandom();
        ConcertScheduled concertScheduled = new ConcertScheduled(
                concertId, 0L, "Artist Name", 50, 
                LocalDateTime.now().plusDays(30), 
                LocalTime.of(18, 30), 
                100, 4);
        TicketsSold ticketsSold = new TicketsSold(
                concertId, 1L, 5, 250);
        Concert reconstitutedConcert = Concert.reconstitute(List.of(concertScheduled,
                                                                   ticketsSold));

        reconstitutedConcert.sellTicketsTo(CustomerId.createRandom(), 3);

        assertThat(reconstitutedConcert.uncommittedEvents())
                .extracting(Event::eventSequence)
                .containsExactly(2L);
    }
}
