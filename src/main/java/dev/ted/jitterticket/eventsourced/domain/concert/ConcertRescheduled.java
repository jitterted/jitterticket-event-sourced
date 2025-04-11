package dev.ted.jitterticket.eventsourced.domain.concert;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertRescheduled(ConcertId concertId,
                                 LocalDateTime newShowDateTime,
                                 LocalTime newDoorsTime)
        implements ConcertEvent {
}
