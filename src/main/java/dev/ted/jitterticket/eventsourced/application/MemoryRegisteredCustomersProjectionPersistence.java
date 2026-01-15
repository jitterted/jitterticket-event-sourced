package dev.ted.jitterticket.eventsourced.application;

// have STATE implement a State interface that supports .isEmpty() and .merge(), which is used to merge the previous snapshot with the delta
public class MemoryRegisteredCustomersProjectionPersistence implements ProjectionPersistencePort<RegisteredCustomers, RegisteredCustomers> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private RegisteredCustomers state = new RegisteredCustomers();

    @Override
    public Snapshot<RegisteredCustomers> loadSnapshot() {
        return new Snapshot<>(state, checkpoint);
    }

    @Override
    public void saveDelta(RegisteredCustomers delta, Checkpoint newCheckpoint) {
        state = state.withNew(delta);
        checkpoint = newCheckpoint;
    }
}
