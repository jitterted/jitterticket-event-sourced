package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class Customer extends EventSourcedAggregate<CustomerEvent, CustomerId> {

    private String name;
    private String email;
    private final List<TicketOrder> ticketOrders = new ArrayList<>();

    public static Customer register(CustomerId customerId, String name, String email) {
        return new Customer(customerId, name, email);
    }

    public static Customer reconstitute(List<CustomerEvent> customerEvents) {
        return new Customer(customerEvents);
    }

    private Customer(List<CustomerEvent> customerEvents) {
        customerEvents.forEach(this::apply);
    }

    private Customer(CustomerId customerId, String name, String email) {
        enqueue(new CustomerRegistered(customerId, name, email));
    }

    public Optional<TicketOrder> ticketOrderFor(TicketOrderId ticketOrderId) {
        return ticketOrders()
                .stream()
                .filter(order -> order.ticketOrderId().equals(ticketOrderId))
                .findFirst();
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {
        switch (customerEvent) {
            case CustomerRegistered(
                    CustomerId customerId,
                    String customerName,
                    String email
            ) -> {
                setId(customerId);
                this.name = customerName;
                this.email = email;
            }

            case TicketsPurchased(
                    _,
                    TicketOrderId ticketOrderId,
                    ConcertId concertId,
                    int quantity,
                    int paidAmount
            ) -> {
                ticketOrders.add(
                        new TicketOrder(ticketOrderId, concertId,
                                        quantity, paidAmount));
            }
        }
    }

    public void purchaseTickets(Concert concert, TicketOrderId ticketOrderId, int quantity) {
        int paidAmount = quantity * concert.ticketPrice();
        TicketsPurchased ticketsPurchased =
                new TicketsPurchased(getId(), ticketOrderId, concert.getId(), quantity, paidAmount);
        enqueue(ticketsPurchased);
    }

    public List<TicketOrder> ticketOrders() {
        return ticketOrders;
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
