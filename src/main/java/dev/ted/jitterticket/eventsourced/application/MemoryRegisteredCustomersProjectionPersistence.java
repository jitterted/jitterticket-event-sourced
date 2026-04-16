package dev.ted.jitterticket.eventsourced.application;

// have STATE implement a State interface that supports .isEmpty() and .merge(), which is used to merge the previous snapshot with the delta
public class MemoryRegisteredCustomersProjectionPersistence
        implements ProjectionPersistencePort<RegisteredCustomers, RegisteredCustomers> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private RegisteredCustomers state = new RegisteredCustomers();

    @Override
    public Checkpointed<RegisteredCustomers> loadSnapshot() {
        return new Checkpointed<>(state, checkpoint);
    }

    @Override
    public NewDomainProjector<RegisteredCustomers, RegisteredCustomers> loadProjector() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveDelta(Checkpointed<RegisteredCustomers> checkpointed) {
        state = state.withNew(checkpointed.state());
        this.checkpoint = checkpointed.checkpoint();
    }
}
