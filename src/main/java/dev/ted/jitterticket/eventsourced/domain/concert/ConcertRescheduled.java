package dev.ted.jitterticket.eventsourced.domain.concert;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertRescheduled(ConcertId concertId,
                                 Integer eventSequence,
                                 LocalDateTime newShowDateTime,
                                 LocalTime newDoorsTime)
        implements ConcertEvent {
}
