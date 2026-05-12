package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;

public class NewRegisteredCustomersProjector
        extends NewDomainProjector<AllRegisteredCustomers, NewlyRegisteredCustomers> {

    private final AllRegisteredCustomers currentState;
    private NewlyRegisteredCustomers deltaState = new NewlyRegisteredCustomers();
    private Checkpoint checkpoint;

    @Deprecated // any non-empty state must, by definition, have a Checkpoint that is not INITIAL, i.e., it MUST have processed at least ONE event for there to be a non-empty STATE
    public NewRegisteredCustomersProjector(AllRegisteredCustomers initialState) {
        this.currentState = AllRegisteredCustomers.copyOf(initialState);
        checkpoint = Checkpoint.INITIAL;
    }

    public NewRegisteredCustomersProjector(Checkpointed<AllRegisteredCustomers> snapshot) {
        this.currentState = AllRegisteredCustomers.copyOf(snapshot.state());
        this.checkpoint = snapshot.checkpoint();
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
    public Checkpointed<NewlyRegisteredCustomers> flush() {
        NewlyRegisteredCustomers uncommittedDelta = deltaState;
        deltaState = new NewlyRegisteredCustomers();
        return new Checkpointed<>(uncommittedDelta, checkpoint);
    }

    @Override
    protected Checkpoint checkpoint() {
        return checkpoint;
    }

}
