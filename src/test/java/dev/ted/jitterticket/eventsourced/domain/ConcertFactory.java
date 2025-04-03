package dev.ted.jitterticket.eventsourced.domain;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class ConcertFactory {
    public static Concert createConcert() {
        return Concert.schedule(new ConcertId(UUID.randomUUID()),
                                "Headliner",
                                99,
                                LocalDateTime.now(),
                                LocalTime.now().minusHours(1),
                                100,
                                4
        );
    }
}
