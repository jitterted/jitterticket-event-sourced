package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RegisteredCustomersProjector implements EventConsumer<CustomerEvent> {
    private final List<RegisteredCustomer> customerSummaries = new ArrayList<>();
    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;

    public RegisteredCustomersProjector(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        this.customerStore = customerStore;
        Stream<CustomerEvent> allCustomerEvents = this.customerStore
                .allEventsAfter(0L);
        this.customerSummaries.addAll(registeredCustomers(allCustomerEvents));
        this.customerStore.subscribe(this);
    }

    @Override
    public void handle(Stream<CustomerEvent> eventStream) {
        customerSummaries.addAll(registeredCustomers(eventStream));
    }

    public Stream<RegisteredCustomer> allCustomers() {
        return customerSummaries.stream();
    }

    private List<RegisteredCustomer> registeredCustomers(Stream<CustomerEvent> allCustomerEvents) {
        return allCustomerEvents
                .gather(Gatherers.filterAndCastTo(CustomerRegistered.class))
                .map(registered -> new RegisteredCustomer(
                        registered.customerId(),
                        registered.customerName()))
                .toList();
    }

    public record RegisteredCustomer(CustomerId customerId, String name) {}
}
