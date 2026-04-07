package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

public class NewRegisteredCustomersProjector
        extends NewDomainProjector<RegisteredCustomers, RegisteredCustomers> {

    private final RegisteredCustomers currentState;
    private RegisteredCustomers deltaState;

    public NewRegisteredCustomersProjector(RegisteredCustomers initialState) {
        this.currentState = RegisteredCustomers.copyOf(initialState);
        deltaState = new RegisteredCustomers();
    }

    public void handle(CustomerRegistered customerRegistered) {
        var registeredCustomer = new RegisteredCustomers.RegisteredCustomer(
                customerRegistered.customerId(),
                customerRegistered.customerName());
        currentState.add(registeredCustomer);
        deltaState.add(registeredCustomer);
    }

   @Override
    public RegisteredCustomers currentState() {
        return currentState;
    }

    @Override
    public RegisteredCustomers flush() {
        RegisteredCustomers uncommittedDelta = deltaState;
        deltaState = new RegisteredCustomers();
        return uncommittedDelta;
    }

}
