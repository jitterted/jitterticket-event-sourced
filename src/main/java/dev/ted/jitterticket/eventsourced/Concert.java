package dev.ted.jitterticket.eventsourced;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Concert {

    private final List<ConcertEvent> uncommittedEvents = new ArrayList<>();
    private int ticketPrice;
    private LocalDateTime showDateTime;
    private LocalTime doorsTime;
    private int capacity;
    private int maxTicketsPerPurchase;

    private Concert(List<ConcertEvent> concertEvents) {
        concertEvents.forEach(this::apply);
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

    private Concert(int ticketPrice, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) {
        ConcertScheduled concertScheduled = new ConcertScheduled(
                ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
        );
        enqueue(concertScheduled);
    }

    private void enqueue(ConcertEvent concertEvent) {
        uncommittedEvents.add(concertEvent);
    }

    private void apply(ConcertEvent concertEvent) {
        switch (concertEvent) {
            case ConcertScheduled(int ticketPrice, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) -> {
                this.ticketPrice = ticketPrice;
                this.showDateTime = showDateTime;
                this.doorsTime = doorsTime;
                this.capacity = capacity;
                this.maxTicketsPerPurchase = maxTicketsPerPurchase;
            }

            case ConcertRescheduled(LocalDateTime newShowDateTime,
                                    LocalTime newDoorsTime)
                    -> {
                this.showDateTime = newShowDateTime;
                this.doorsTime = newDoorsTime;
            }
        }
    }

    public List<ConcertEvent> uncommittedEvents() {
        return uncommittedEvents;
    }

    public int ticketPrice() {
        return ticketPrice;
    }

    public LocalDateTime showDateTime() {
        return showDateTime;
    }

    public LocalTime doorsTime() {
        return doorsTime;
    }

    public int capacity() {
        return capacity;
    }

    public int maxTicketsPerPurchase() {
        return maxTicketsPerPurchase;
    }

    public void rescheduleTo(LocalDateTime newShowDateTime, LocalTime newDoorsTime) {
        ConcertRescheduled concertRescheduled = new ConcertRescheduled(newShowDateTime, newDoorsTime);
        enqueue(concertRescheduled);
    }

}
