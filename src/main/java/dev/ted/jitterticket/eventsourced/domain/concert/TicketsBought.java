package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

public record TicketsBought(ConcertId concertId,
                            CustomerId customerId,
                            int quantity) implements ConcertEvent {
}
