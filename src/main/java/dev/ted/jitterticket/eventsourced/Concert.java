package dev.ted.jitterticket.eventsourced;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class Concert {

    public static Concert schedule(int price, LocalDateTime showDateTime, LocalTime doorsTime, int capacity, int maxTicketsPerPurchase) {
        return new Concert();
    }

    public List<ConcertEvent> uncommittedEvents() {
        return List.of(new ConcertScheduled(
                0, LocalDateTime.now(), LocalTime.now(), 0, 0
        ));
    }
}
