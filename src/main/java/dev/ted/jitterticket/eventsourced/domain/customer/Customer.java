package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.*;

public class Customer extends EventSourcedAggregate<CustomerEvent, CustomerId> {

    private String name;
    private String email;
    private final Map<TicketOrderId, TicketOrder> ticketOrdersByTicketOrderId = new HashMap<>();

    //region Creation Command
    public static Customer register(CustomerId customerId, String name, String email) {
        return new Customer(customerId, name, email);
    }
    //endregion

    //region Event Application
    public static Customer reconstitute(List<CustomerEvent> customerEvents) {
        return new Customer(customerEvents);
    }

    private Customer(List<CustomerEvent> customerEvents) {
        applyAll(customerEvents);
    }

    private Customer(CustomerId customerId, String name, String email) {
        enqueue(new CustomerRegistered(customerId, nextEventSequenceNumber(), name, email));
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {
        switch (customerEvent) {
            case CustomerRegistered registered -> {
                setId(registered.customerId());
                this.name = registered.customerName();
                this.email = registered.email();
            }
            case TicketsPurchased purchased -> {
                TicketOrder ticketOrder = new TicketOrder(
                        purchased.ticketOrderId(), purchased.concertId(), purchased.quantity(), purchased.paidAmount());
                ticketOrdersByTicketOrderId.put(purchased.ticketOrderId(), ticketOrder);
            }
        }
    }
    //endregion

    //region Queries
    public void purchaseTickets(Concert concert, TicketOrderId ticketOrderId, int quantity) {
        int paidAmount = quantity * concert.ticketPrice();
        TicketsPurchased ticketsPurchased =
                new TicketsPurchased(getId(), nextEventSequenceNumber(), ticketOrderId, concert.getId(), quantity, paidAmount);
        enqueue(ticketsPurchased);
    }

    public List<TicketOrder> ticketOrders() {
        return ticketOrdersByTicketOrderId.values().stream().toList();
    }

    public Optional<TicketOrder> ticketOrderFor(TicketOrderId ticketOrderId) {
        return Optional.ofNullable(ticketOrdersByTicketOrderId.get(ticketOrderId));
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }
    //endregion Queries

    @Override
    public String toString() {
        return new StringJoiner(", ", Customer.class.getSimpleName() + "[", "]")
                .add("id='" + getId() + "'")
                .add("name='" + name + "'")
                .add("email='" + email + "'")
                .toString();
    }

    public record TicketOrder(TicketOrderId ticketOrderId,
                              ConcertId concertId,
                              int quantity,
                              int amountPaid) {}
}
