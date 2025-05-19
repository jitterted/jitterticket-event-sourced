package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.application.RegisteredCustomersProjector;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.util.List;
import java.util.UUID;

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
        return projector.allCustomers()
                .map(customerSummary -> new AggregateSummaryView(
                        customerSummary.customerId().toString(),
                        customerSummary.name()
                ))
                .toList();
    }

    @Override
    public List<? extends Event> eventsFor(UUID uuid) {
        return List.of();
    }

    @Override
    public List<String> propertiesOfProjectionFrom(List<? extends Event> events) {
        return List.of();
    }
}
