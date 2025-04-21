package dev.ted.jitterticket.eventsourced.domain.concert;

import java.time.LocalDateTime;
import java.time.LocalTime;

public class ConcertFactory {
    public static Concert createConcert() {
        ConcertId concertId = ConcertId.createRandom();
        return createConcertWithId(concertId);
    }

    public static Concert createConcertWithId(ConcertId concertId) {
        return Concert.schedule(concertId,
                                "Headliner",
                                99,
                                LocalDateTime.now(),
                                LocalTime.now().minusHours(1),
                                100,
                                4
        );
    }

    public static Concert createConcertWith(ConcertId concertId, String artist, int ticketPrice, LocalDateTime showDateTime, LocalTime doorsTime) {
        return Concert.schedule(concertId,
                                artist,
                                ticketPrice,
                                showDateTime,
                                doorsTime,
                                100,
                                4
        );
    }

    public static Concert createWithCapacity(int capacity) {
        return Concert.schedule(ConcertId.createRandom(),
                         "Headliner",
                         99,
                         LocalDateTime.now(),
                         LocalTime.now().minusHours(1),
                         capacity,
                         4
        );
    }

    public static Concert withTicketPriceOf(int ticketPrice) {
        return Concert.schedule(ConcertId.createRandom(),
                                "Headliner",
                                ticketPrice,
                                LocalDateTime.now(),
                                LocalTime.now().minusHours(1),
                                100,
                                4
        );
    }

    static class Events {

        static ConcertScheduled createConcertScheduledEventWithCapacityOf(int capacity, int ticketPrice) {
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            int maxTicketsPerPurchase = 4;
            String artist = "Irrelevant Artist Name";
            return new ConcertScheduled(ConcertId.createRandom(), artist, ticketPrice, LocalDateTime.of(2025, 11, 11, 20, 0), originalDoorsTime, capacity, maxTicketsPerPurchase);
        }

        static ConcertScheduled createConcertScheduledEvent(LocalDateTime originalShowDateTime, LocalTime originalDoorsTime) {
            int ticketPrice = 35;
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Irrelevant Artist Name";
            return new ConcertScheduled(ConcertId.createRandom(), artist, ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase);
        }

        static ConcertScheduled scheduleConcertWithCapacityOf(int capacity) {
            return createConcertScheduledEventWithCapacityOf(capacity, 35);
        }
    }

}
