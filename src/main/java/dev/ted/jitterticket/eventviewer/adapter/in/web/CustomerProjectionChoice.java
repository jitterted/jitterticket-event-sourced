package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.RegisteredCustomersProjector;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class CustomerProjectionChoice extends ProjectionChoice {
    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;
    private final RegisteredCustomersProjector projector;

    public CustomerProjectionChoice(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        super("Customer", "customers");
        this.customerStore = customerStore;
        this.projector = new RegisteredCustomersProjector(customerStore);
    }

    @Override
    public List<AggregateSummaryView> aggregateSummaryViews() {
        return projector.allCustomers().asStream()
                        .map(customerSummary -> new AggregateSummaryView(
                                customerSummary.customerId().id().toString(),
                                customerSummary.name()
                        ))
                        .toList();
    }

    @Override
    public List<? extends Event> eventsFor(UUID uuid) {
        return customerStore.eventsForAggregate(new CustomerId(uuid));
    }

    @Override
    public List<String> propertiesOfProjectionFrom(List<? extends Event> events) {
        @SuppressWarnings("unchecked")
        Customer customer = Customer.reconstitute((List<CustomerEvent>) events);
        return List.of(
                "Customer Name: " + customer.name(),
                "Email: " + customer.email(),
                "Ticket Orders: " + ticketOrdersAsString(customer.ticketOrders()));
    }

    private String ticketOrdersAsString(List<Customer.TicketOrder> ticketOrders) {
        if (ticketOrders.isEmpty()) {
            return "No Ticket Orders";
        }
        return ticketOrders.stream()
                           .map(this::orderAsString)
                           .collect(Collectors.joining("<br/>"));
    }

    private String orderAsString(Customer.TicketOrder order) {
        return order.ticketOrderId().toString()
               + ", " + order.concertId().toString() + ": "
               + " Quantity = " + order.quantity()
               + ", Paid: " + order.amountPaid();
    }
}
