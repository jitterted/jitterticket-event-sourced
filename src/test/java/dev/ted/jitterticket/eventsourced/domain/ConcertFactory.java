package dev.ted.jitterticket.eventsourced.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class ConcertFactory {
    public static Concert createConcert() {
        ConcertId concertId = new ConcertId(UUID.randomUUID());
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

    public static Concert createConcertWith(ConcertId firstConcertId, String artist, int ticketPrice, LocalDateTime showDateTime, LocalTime doorsTime) {
        return Concert.schedule(firstConcertId,
                                artist,
                                ticketPrice,
                                showDateTime,
                                doorsTime,
                                100,
                                4
        );
    }
}
