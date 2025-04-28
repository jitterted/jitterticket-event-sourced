package dev.ted.jitterticket.eventsourced.domain.concert;

public record TicketsSold(ConcertId concertId,
                          Long eventSequence,
                          int quantity,
                          int totalPaid) implements ConcertEvent {
}
