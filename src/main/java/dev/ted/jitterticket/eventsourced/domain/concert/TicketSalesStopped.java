package dev.ted.jitterticket.eventsourced.domain.concert;

import java.util.Objects;
import java.util.StringJoiner;

public final class TicketSalesStopped extends ConcertEvent {

    public TicketSalesStopped(ConcertId concertId, Long eventSequence) {
        super(concertId, eventSequence);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TicketSalesStopped that = (TicketSalesStopped) o;
        return Objects.equals(concertId, that.concertId) &&
               Objects.equals(eventSequence, that.eventSequence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(concertId, eventSequence);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TicketSalesStopped.class.getSimpleName() + "[", "]")
                .add("concertId=" + concertId)
                .add("eventSequence=" + eventSequence)
                .toString();
    }
}
