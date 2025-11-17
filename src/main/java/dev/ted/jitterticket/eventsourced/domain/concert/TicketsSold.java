package dev.ted.jitterticket.eventsourced.domain.concert;

import java.util.Objects;
import java.util.StringJoiner;

public final class TicketsSold extends ConcertEvent {
    private final int quantity;
    private final int totalPaid;

    public static TicketsSold createNew(ConcertId concertId,
                                        Integer eventSequence,
                                        int quantity,
                                        int totalPaid) {
        return new TicketsSold(concertId, eventSequence, quantity, totalPaid);
    }

    private TicketsSold(ConcertId concertId,
                        Integer eventSequence,
                        int quantity,
                        int totalPaid) {
        super(concertId, eventSequence);
        this.quantity = quantity;
        this.totalPaid = totalPaid;
    }

    public int quantity() {
        return quantity;
    }

    public int totalPaid() {
        return totalPaid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketsSold that = (TicketsSold) o;
        return quantity == that.quantity &&
               totalPaid == that.totalPaid &&
               Objects.equals(concertId(), that.concertId()) &&
               Objects.equals(eventSequence(), that.eventSequence());
    }

    @Override
    public int hashCode() {
        return Objects.hash(concertId(), eventSequence(), quantity, totalPaid);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TicketsSold.class.getSimpleName() + "[", "]")
                .add("concertId='" + concertId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("quantity=" + quantity)
                .add("totalPaid=" + totalPaid)
                .toString();
    }
}
