package dev.ted.jitterticket.eventsourced.domain.customer;

import java.util.List;

public class CustomerFactory {

    public static Customer reconstituteWithRegisteredEvent() {
        CustomerRegistered customerRegistered = new CustomerRegistered(
                CustomerId.createRandom(), 0L, "customer name", "email@example.com");
        return Customer.reconstitute(List.of(customerRegistered));
    }

    public static Customer newlyRegistered() {
        return Customer.register(
                CustomerId.createRandom(), "customer name", "email@example.com");
    }

}
