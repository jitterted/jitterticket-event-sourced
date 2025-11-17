package dev.ted.jitterticket.eventsourced.domain.customer;

import java.util.List;

public class CustomerFactory {

    public static Customer reconstituteWithRegisteredEvent() {
        CustomerRegistered customerRegistered = CustomerRegistered.createNew(
                CustomerId.createRandom(), 0, "customer name", "email@example.com");
        return Customer.reconstitute(List.of(customerRegistered));
    }

    public static Customer newlyRegistered() {
        return Customer.register(
                CustomerId.createRandom(), "customer name", "email@example.com");
    }

}
