package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

import java.util.stream.Stream;

public class RegisteredCustomersProjector {
    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;

    public RegisteredCustomersProjector(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        this.customerStore = customerStore;
    }

    public Stream<CustomerSummary> allCustomers() {
        return customerStore.allEvents()
                            .gather(Gatherers.filterAndCastTo(CustomerRegistered.class))
                            .map(registered -> new CustomerSummary(
                                    registered.customerId(),
                                    registered.customerName()
                            ));
    }

}
