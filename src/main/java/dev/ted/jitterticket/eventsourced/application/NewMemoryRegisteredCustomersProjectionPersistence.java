package dev.ted.jitterticket.eventsourced.application;

public class NewMemoryRegisteredCustomersProjectionPersistence
        implements ProjectionPersistencePort<AllRegisteredCustomers, NewlyRegisteredCustomers> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private final AllRegisteredCustomers state = new AllRegisteredCustomers();

    @Override
    public Snapshot<AllRegisteredCustomers> loadSnapshot() {
        return new Snapshot<>(state, checkpoint);
    }

    @Override
    public void saveDelta(NewlyRegisteredCustomers delta,
                          Checkpoint newCheckpoint) {
        state.add(delta.asList(), newCheckpoint);
        checkpoint = newCheckpoint;
    }
}
