package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

import java.util.List;
import java.util.stream.Stream;

public class RegisteredCustomersProjector implements
        DomainProjector<CustomerEvent, RegisteredCustomers, RegisteredCustomers> {

    @Override
    public ProjectorResult<RegisteredCustomers, RegisteredCustomers> project(
            RegisteredCustomers currentState,
            Stream<CustomerEvent> customerEventStream) {
        List<RegisteredCustomers.RegisteredCustomer> newlyRegisteredCustomers =
                customerEventStream
                        .gather(Gatherers.filterAndCastTo(CustomerRegistered.class))
                        .map(registered -> new RegisteredCustomers.RegisteredCustomer(
                                registered.customerId(),
                                registered.customerName()))
                        .toList();

        RegisteredCustomers newState = currentState.withNew(newlyRegisteredCustomers);

        return new ProjectorResult<>(newState,
                                     new RegisteredCustomers(newlyRegisteredCustomers));
    }

}
