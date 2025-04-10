package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.StringJoiner;

public class Concert extends EventSourcedAggregate<ConcertEvent, ConcertId> {

    private String artist;
    private int ticketPrice;
    private LocalDateTime showDateTime;
    private LocalTime doorsTime;
    private int capacity;
    private int maxTicketsPerPurchase;
    private int availableTicketCount;

    private Concert(List<ConcertEvent> concertEvents) {
        concertEvents.forEach(this::apply);
    }

    public static Concert schedule(ConcertId concertId,
                                   String artist,
                                   int ticketPrice,
                                   LocalDateTime showDateTime,
                                   LocalTime doorsTime,
                                   int capacity,
                                   int maxTicketsPerPurchase) {
        return new Concert(concertId, artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
    }

    public static Concert reconstitute(List<ConcertEvent> concertEvents) {
        return new Concert(concertEvents);
    }

    private Concert(ConcertId concertId, String artist, int ticketPrice, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) {
        ConcertScheduled concertScheduled = new ConcertScheduled(
                concertId, artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
        );
        enqueue(concertScheduled);
    }

    @Override
    protected void apply(ConcertEvent concertEvent) {
        switch (concertEvent) {
            case ConcertScheduled(
                    ConcertId concertId,
                    String artist,
                    int ticketPrice,
                    LocalDateTime showDateTime,
                    LocalTime doorsTime,
                    int capacity,
                    int maxTicketsPerPurchase
            ) -> {
                this.setId(concertId);
                this.artist = artist;
                this.ticketPrice = ticketPrice;
                this.showDateTime = showDateTime;
                this.doorsTime = doorsTime;
                this.capacity = capacity;
                this.availableTicketCount = capacity;
                this.maxTicketsPerPurchase = maxTicketsPerPurchase;
            }

            case ConcertRescheduled(
                    ConcertId concertId, LocalDateTime newShowDateTime,
                    LocalTime newDoorsTime
            ) -> {
                this.showDateTime = newShowDateTime;
                this.doorsTime = newDoorsTime;
            }

            case TicketsBought(ConcertId concertId, _, int quantity) ->
                    this.availableTicketCount -= quantity;
        }
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

    public String artist() {
        return artist;
    }

    public void rescheduleTo(LocalDateTime newShowDateTime,
                             LocalTime newDoorsTime) {
        // validation: new times must be X amount of time in the future
        ConcertRescheduled concertRescheduled =
                new ConcertRescheduled(getId(), newShowDateTime, newDoorsTime);
        enqueue(concertRescheduled);
    }

    public int availableTicketCount() {
        return availableTicketCount;
    }

    public void buyTickets(CustomerId customerId, int quantity) {
        TicketsBought ticketsBought = new TicketsBought(getId(), customerId, quantity);
        enqueue(ticketsBought);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Concert.class.getSimpleName() + "[", "]")
                .add("(id='" + getId() + "')")
                .add("artist='" + artist + "'")
                .add("ticketPrice=" + ticketPrice)
                .add("showDateTime=" + showDateTime)
                .add("doorsTime=" + doorsTime)
                .add("capacity=" + capacity)
                .add("maxTicketsPerPurchase=" + maxTicketsPerPurchase)
                .add("availableTicketCount=" + availableTicketCount)
                .toString();
    }
}
