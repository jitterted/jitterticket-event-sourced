package dev.ted.jitterticket.eventsourced.domain.concert;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.StringJoiner;

public final class ConcertScheduled extends ConcertEvent {
    private final String artist;
    private final int ticketPrice;
    private final LocalDateTime showDateTime;
    private final LocalTime doorsTime;
    private final int capacity;
    private final int maxTicketsPerPurchase;

    public ConcertScheduled(ConcertId concertId,
                            Integer eventSequence,
                            String artist,
                            int ticketPrice,
                            LocalDateTime showDateTime,
                            LocalTime doorsTime,
                            int capacity,
                            int maxTicketsPerPurchase) {
        super(concertId, eventSequence);
        this.artist = artist;
        this.ticketPrice = ticketPrice;
        this.showDateTime = showDateTime;
        this.doorsTime = doorsTime;
        this.capacity = capacity;
        this.maxTicketsPerPurchase = maxTicketsPerPurchase;
    }

    public String artist() {
        return artist;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcertScheduled that = (ConcertScheduled) o;
        return ticketPrice == that.ticketPrice &&
               capacity == that.capacity &&
               maxTicketsPerPurchase == that.maxTicketsPerPurchase &&
               Objects.equals(concertId(), that.concertId()) &&
               Objects.equals(eventSequence(), that.eventSequence()) &&
               Objects.equals(artist, that.artist) &&
               Objects.equals(showDateTime, that.showDateTime) &&
               Objects.equals(doorsTime, that.doorsTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concertId(), eventSequence(), artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConcertScheduled.class.getSimpleName() + "[", "]")
                .add("concertId='" + concertId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("artist='" + artist + "'")
                .add("ticketPrice=" + ticketPrice)
                .add("showDateTime=" + showDateTime)
                .add("doorsTime=" + doorsTime)
                .add("capacity=" + capacity)
                .add("maxTicketsPerPurchase=" + maxTicketsPerPurchase)
                .toString();
    }
}
