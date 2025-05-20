package dev.ted.jitterticket.eventsourced.domain.concert;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.StringJoiner;

public final class ConcertRescheduled extends ConcertEvent {
    private final LocalDateTime newShowDateTime;
    private final LocalTime newDoorsTime;

    public ConcertRescheduled(ConcertId concertId,
                              Integer eventSequence,
                              LocalDateTime newShowDateTime,
                              LocalTime newDoorsTime) {
        super(concertId, eventSequence);
        this.newShowDateTime = newShowDateTime;
        this.newDoorsTime = newDoorsTime;
    }

    public LocalDateTime newShowDateTime() {
        return newShowDateTime;
    }

    public LocalTime newDoorsTime() {
        return newDoorsTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConcertRescheduled that = (ConcertRescheduled) o;
        return Objects.equals(concertId(), that.concertId()) &&
               Objects.equals(eventSequence(), that.eventSequence()) &&
               Objects.equals(newShowDateTime, that.newShowDateTime) &&
               Objects.equals(newDoorsTime, that.newDoorsTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concertId(), eventSequence(), newShowDateTime, newDoorsTime);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ConcertRescheduled.class.getSimpleName() + "[", "]")
                .add("concertId='" + concertId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("newShowDateTime=" + newShowDateTime)
                .add("newDoorsTime=" + newDoorsTime)
                .toString();
    }
}
