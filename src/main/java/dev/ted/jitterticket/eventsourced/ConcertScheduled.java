package dev.ted.jitterticket.eventsourced;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertScheduled(int ticketPrice,
                               LocalDateTime showDateTime,
                               LocalTime doorsTime,
                               int capacity,
                               int maxTicketsPerPurchase)
        implements ConcertEvent {
}
