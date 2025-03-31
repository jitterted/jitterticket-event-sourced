package dev.ted.jitterticket.eventsourced;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertRescheduled(LocalDateTime newShowDateTime,
                                 LocalTime newDoorsTime)
        implements ConcertEvent {
}
