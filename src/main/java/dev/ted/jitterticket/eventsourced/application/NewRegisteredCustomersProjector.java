package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

public class NewRegisteredCustomersProjector
        extends NewDomainProjector<AllRegisteredCustomers, NewlyRegisteredCustomers> {

    private final AllRegisteredCustomers currentState;
    private NewlyRegisteredCustomers deltaState;
    private Checkpoint checkpoint = Checkpoint.INITIAL;

    public NewRegisteredCustomersProjector(AllRegisteredCustomers initialState) {
        this.currentState = AllRegisteredCustomers.copyOf(initialState);
        deltaState = new NewlyRegisteredCustomers();
    }

    public static NewRegisteredCustomersProjector createEmpty() {
        return new NewRegisteredCustomersProjector(
                new AllRegisteredCustomers());
    }

    public void handle(CustomerRegistered customerRegistered) {
        var registeredCustomer = new RegisteredCustomer(
                customerRegistered.customerId(),
                customerRegistered.customerName());
        checkpoint = Checkpoint.of(customerRegistered.eventSequence());
        currentState.add(registeredCustomer);
        deltaState.add(registeredCustomer);
    }

    @Override
    public Checkpointed<AllRegisteredCustomers> projection() {
        return new Checkpointed<>(currentState, checkpoint);
    }

    @Override
    protected Checkpoint checkpoint() {
        return checkpoint;
    }

    @Override
    // Snapshot
    public Checkpointed<NewlyRegisteredCustomers> flush() {
        NewlyRegisteredCustomers uncommittedDelta = deltaState;
        deltaState = new NewlyRegisteredCustomers();
        return new Checkpointed<>(uncommittedDelta, checkpoint);
    }

}
