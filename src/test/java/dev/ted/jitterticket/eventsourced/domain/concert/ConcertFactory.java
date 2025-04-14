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
}
