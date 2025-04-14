package dev.ted.jitterticket.eventsourced.domain.concert;

public record TicketsSold(ConcertId concertId,
                          int quantity,
                          int totalPaid) implements ConcertEvent {
}
