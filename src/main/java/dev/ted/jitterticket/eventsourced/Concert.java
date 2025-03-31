package dev.ted.jitterticket.eventsourced;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Concert {

    private final List<ConcertEvent> concertEvents = new ArrayList<>();

    public static Concert schedule(int price,
                                   LocalDateTime showDateTime,
                                   LocalTime doorsTime,
                                   int capacity,
                                   int maxTicketsPerPurchase) {
        return new Concert(price, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
    }

    private Concert(int price, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) {
        ConcertScheduled concertScheduled = new ConcertScheduled(
                price, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
        );
        concertEvents.add(concertScheduled);
    }

    public List<ConcertEvent> uncommittedEvents() {
        return concertEvents;
    }
}
