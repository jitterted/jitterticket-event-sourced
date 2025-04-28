package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

public record TicketsPurchased(CustomerId customerId,
                               Long eventSequence,
                               TicketOrderId ticketOrderId,
                               ConcertId concertId,
                               int quantity,
                               int paidAmount) implements CustomerEvent {
}
