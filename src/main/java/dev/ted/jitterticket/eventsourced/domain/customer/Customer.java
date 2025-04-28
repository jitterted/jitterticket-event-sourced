package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

public class Customer extends EventSourcedAggregate<CustomerEvent, CustomerId> {

    private String name;
    private String email;
    private final Map<TicketOrderId, TicketOrder> ticketOrdersByTicketOrderId = new HashMap<>();

    public static Customer register(CustomerId customerId, String name, String email) {
        return new Customer(customerId, name, email);
    }

    public static Customer reconstitute(List<CustomerEvent> customerEvents) {
        return new Customer(customerEvents);
    }

    private Customer(List<CustomerEvent> customerEvents) {
        applyAll(customerEvents);
    }


    private Customer(CustomerId customerId, String name, String email) {
        enqueue(new CustomerRegistered(customerId, nextEventSequenceNumber(), name, email));
    }

    public Optional<TicketOrder> ticketOrderFor(TicketOrderId ticketOrderId) {
        return Optional.ofNullable(ticketOrdersByTicketOrderId.get(ticketOrderId));
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {
        switch (customerEvent) {
            case CustomerRegistered(
                    CustomerId customerId, _,
                    String customerName,
                    String email
            ) -> {
                setId(customerId);
                this.name = customerName;
                this.email = email;
            }

            case TicketsPurchased(_, _,
                    TicketOrderId ticketOrderId,
                    ConcertId concertId,
                    int quantity,
                    int paidAmount
            ) -> {
                TicketOrder ticketOrder = new TicketOrder(
                        ticketOrderId, concertId, quantity, paidAmount);
                ticketOrdersByTicketOrderId.put(ticketOrderId, ticketOrder);
            }
        }
    }

    public void purchaseTickets(Concert concert, TicketOrderId ticketOrderId, int quantity) {
        int paidAmount = quantity * concert.ticketPrice();
        TicketsPurchased ticketsPurchased =
                new TicketsPurchased(getId(), nextEventSequenceNumber(), ticketOrderId, concert.getId(), quantity, paidAmount);
        enqueue(ticketsPurchased);
    }

    public List<TicketOrder> ticketOrders() {
        return ticketOrdersByTicketOrderId.values().stream().toList();
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

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
