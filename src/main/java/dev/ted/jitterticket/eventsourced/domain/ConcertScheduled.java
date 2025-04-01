package dev.ted.jitterticket.eventsourced.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertScheduled(String artist, int ticketPrice,
                               LocalDateTime showDateTime,
                               LocalTime doorsTime,
                               int capacity,
                               int maxTicketsPerPurchase
)
        implements ConcertEvent {
}
