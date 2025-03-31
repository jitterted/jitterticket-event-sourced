package dev.ted.jitterticket.eventsourced;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Concert {

    private final List<ConcertEvent> uncommittedEvents = new ArrayList<>();
    private int ticketPrice = -99;

    private Concert(List<ConcertEvent> concertEvents) {
        ConcertEvent concertEvent = concertEvents.getFirst();
        switch (concertEvent) {
            case ConcertScheduled(int price, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) -> {
                ticketPrice = price;
            }
        }
    }

    public static Concert schedule(int price,
                                   LocalDateTime showDateTime,
                                   LocalTime doorsTime,
                                   int capacity,
                                   int maxTicketsPerPurchase) {
        return new Concert(price, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
    }

    public static Concert reconstitute(List<ConcertEvent> concertEvents) {
        return new Concert(concertEvents);
    }

    private Concert(int price, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) {
        ConcertScheduled concertScheduled = new ConcertScheduled(
                price, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
        );
        uncommittedEvents.add(concertScheduled);
    }

    public List<ConcertEvent> uncommittedEvents() {
        return uncommittedEvents;
    }

    public int ticketPrice() {
        return ticketPrice;
    }
}
