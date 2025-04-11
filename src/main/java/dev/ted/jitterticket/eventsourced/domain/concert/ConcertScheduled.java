package dev.ted.jitterticket.eventsourced.domain.concert;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertScheduled(ConcertId concertId,
                               String artist,
                               int ticketPrice,
                               LocalDateTime showDateTime,
                               LocalTime doorsTime,
                               int capacity,
                               int maxTicketsPerPurchase
)
        implements ConcertEvent {
}
