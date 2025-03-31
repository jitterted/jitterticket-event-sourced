package dev.ted.jitterticket.eventsourced;

public record ConcertScheduled(int price, java.time.LocalDateTime showDateTime,
                               java.time.LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) implements ConcertEvent {
}
