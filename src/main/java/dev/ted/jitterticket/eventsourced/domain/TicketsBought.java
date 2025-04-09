package dev.ted.jitterticket.eventsourced.domain;

public record TicketsBought(ConcertId concertId, CustomerId customerId,
                            int quantity) implements ConcertEvent {
}
