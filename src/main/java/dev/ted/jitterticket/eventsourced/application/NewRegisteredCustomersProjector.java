package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

public class NewRegisteredCustomersProjector
        extends NewDomainProjector<AllRegisteredCustomers, NewlyRegisteredCustomers> {

    private final AllRegisteredCustomers currentState;
    private NewlyRegisteredCustomers deltaState;

    public NewRegisteredCustomersProjector(AllRegisteredCustomers initialState) {
        this.currentState = AllRegisteredCustomers.copyOf(initialState);
        deltaState = new NewlyRegisteredCustomers(Checkpoint.INITIAL);
    }

    public static NewRegisteredCustomersProjector createEmpty() {
        return new NewRegisteredCustomersProjector(
                new AllRegisteredCustomers());
    }

    public void handle(CustomerRegistered customerRegistered) {
        var registeredCustomer = new RegisteredCustomer(
                customerRegistered.customerId(),
                customerRegistered.customerName());
        Checkpoint checkpoint = Checkpoint.of(customerRegistered.eventSequence());
        currentState.add(registeredCustomer, checkpoint);
        deltaState.add(registeredCustomer);
        deltaState.updateCheckpointTo(Checkpoint.of(customerRegistered.eventSequence()));
    }

    @Override
    public AllRegisteredCustomers currentState() {
        return currentState;
    }

    @Override
    public NewlyRegisteredCustomers flush() {
        NewlyRegisteredCustomers uncommittedDelta = deltaState;
        deltaState = new NewlyRegisteredCustomers(deltaState.checkpoint());
        return uncommittedDelta;
    }

    @Override
    public Checkpoint checkpoint() {
        return currentState.checkpoint();
    }

}
