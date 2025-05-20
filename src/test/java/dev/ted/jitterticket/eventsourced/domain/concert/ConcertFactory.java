package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;

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

    public static Concert scheduleConcertWith(ConcertId concertId, String artist, int ticketPrice, LocalDateTime showDateTime, LocalTime doorsTime) {
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

    public static class Store {
        public static ConcertId createSavedConcertIn(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId = ConcertId.createRandom();
            concertStore.save(Concert.schedule(
                    concertId,
                    "Blue Note Quartet",
                    35,
                    LocalDateTime.of(2025, 8, 22, 19, 30),
                    LocalTime.of(18, 30),
                    75,
                    2));
            return concertId;
        }
    }

    public static class Events {

        public static ConcertScheduled scheduledConcertWithCapacityAndTicketPrice(int capacity, int ticketPrice) {
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            int maxTicketsPerPurchase = 4;
            String artist = "Irrelevant Artist Name";
            return new ConcertScheduled(ConcertId.createRandom(), 0, artist, ticketPrice, LocalDateTime.of(2025, 11, 11, 20, 0), originalDoorsTime, capacity, maxTicketsPerPurchase);
        }

        public static ConcertScheduled scheduledConcert(LocalDateTime originalShowDateTime, LocalTime originalDoorsTime) {
            int ticketPrice = 35;
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Irrelevant Artist Name";
            return new ConcertScheduled(ConcertId.createRandom(), 0, artist, ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase);
        }

        public static ConcertScheduled scheduledConcertWithCapacityOf(int capacity) {
            return scheduledConcertWithCapacityAndTicketPrice(capacity, 35);
        }

        public static ConcertScheduled scheduledConcert() {
            return scheduledConcertWithCapacityAndTicketPrice(100, 35);
        }
    }

}
