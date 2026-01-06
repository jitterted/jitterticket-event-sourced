package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

import java.util.List;
import java.util.stream.Stream;

public class RegisteredCustomersProjector implements EventConsumer<CustomerEvent> {
    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;
    private final RegisteredCustomers registeredCustomers = new RegisteredCustomers();

    public RegisteredCustomersProjector(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        this.customerStore = customerStore;
        Stream<CustomerEvent> allCustomerEvents = this.customerStore
                .allEventsAfter(0L);
        registeredCustomers.add(registeredCustomers(allCustomerEvents));
        this.customerStore.subscribe(this);
    }

    @Override
    public void handle(Stream<CustomerEvent> eventStream) {
        registeredCustomers.add(registeredCustomers(eventStream));
    }

    public RegisteredCustomers allCustomers() {
        return registeredCustomers;
    }

    private List<RegisteredCustomers.RegisteredCustomer> registeredCustomers(Stream<CustomerEvent> allCustomerEvents) {
        return allCustomerEvents
                .gather(Gatherers.filterAndCastTo(CustomerRegistered.class))
                .map(registered -> new RegisteredCustomers.RegisteredCustomer(
                        registered.customerId(),
                        registered.customerName()))
                .toList();
    }

}
