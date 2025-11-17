package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.Objects;
import java.util.StringJoiner;

public final class TicketsPurchased extends CustomerEvent {
    private final TicketOrderId ticketOrderId;
    private final ConcertId concertId;
    private final int quantity;
    private final int paidAmount;

    public static TicketsPurchased createNew(CustomerId customerId,
                                             Integer eventSequence,
                                             TicketOrderId ticketOrderId,
                                             ConcertId concertId,
                                             int quantity,
                                             int paidAmount) {
        return new TicketsPurchased(customerId, eventSequence, ticketOrderId, concertId, quantity, paidAmount);
    }

    private TicketsPurchased(CustomerId customerId,
                             Integer eventSequence,
                             TicketOrderId ticketOrderId,
                             ConcertId concertId,
                             int quantity,
                             int paidAmount) {
        super(customerId, eventSequence);
        this.ticketOrderId = ticketOrderId;
        this.concertId = concertId;
        this.quantity = quantity;
        this.paidAmount = paidAmount;
    }

    public TicketOrderId ticketOrderId() {
        return ticketOrderId;
    }

    public ConcertId concertId() {
        return concertId;
    }

    public int quantity() {
        return quantity;
    }

    public int paidAmount() {
        return paidAmount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TicketsPurchased that = (TicketsPurchased) o;
        return quantity == that.quantity &&
               paidAmount == that.paidAmount &&
               Objects.equals(customerId(), that.customerId()) &&
               Objects.equals(eventSequence(), that.eventSequence()) &&
               Objects.equals(ticketOrderId, that.ticketOrderId) &&
               Objects.equals(concertId, that.concertId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId(), eventSequence(), ticketOrderId, concertId, quantity, paidAmount);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", TicketsPurchased.class.getSimpleName() + "[", "]")
                .add("customerId='" + customerId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("ticketOrderId=" + ticketOrderId)
                .add("concertId=" + concertId)
                .add("quantity=" + quantity)
                .add("paidAmount=" + paidAmount)
                .toString();
    }
}
