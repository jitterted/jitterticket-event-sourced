package dev.ted.jitterticket.eventsourced.application;

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
                            .filter(CustomerRegistered.class::isInstance)
                            .map(CustomerRegistered.class::cast)
                            .map(customerEvent -> new CustomerSummary(
                                    customerEvent.customerId(),
                                    customerEvent.customerName()
                            ));
    }
}
