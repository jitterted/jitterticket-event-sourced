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
    public void saveDelta(NewlyRegisteredCustomers delta,
                          Checkpoint newCheckpoint) {
        state.add(delta.asList());
        checkpoint = newCheckpoint;
    }
}
