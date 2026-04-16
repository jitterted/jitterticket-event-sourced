package dev.ted.jitterticket.eventsourced.application;

public class NewMemoryRegisteredCustomersProjectionPersistence
        implements ProjectionPersistencePort<AllRegisteredCustomers, NewlyRegisteredCustomers> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private final AllRegisteredCustomers state = new AllRegisteredCustomers();

    @Override
    public Checkpointed<AllRegisteredCustomers> loadSnapshot() {
        // ensure we return a copy of the state to avoid aliasing errors
        return new Checkpointed<>(AllRegisteredCustomers.copyOf(state), checkpoint);
    }

    @Override
    public NewDomainProjector<AllRegisteredCustomers, NewlyRegisteredCustomers> loadProjector() {
        Checkpointed<AllRegisteredCustomers> snapshot = loadSnapshot();
        return new NewRegisteredCustomersProjector(snapshot);
    }

    @Override
    public void saveDelta(Checkpointed<NewlyRegisteredCustomers> checkpointed) {
        state.add(checkpointed.state().asList());
        this.checkpoint = checkpointed.checkpoint();
    }
}
