package dev.ted.jitterticket.eventsourced.application;

public class MemoryRegisteredCustomersProjectionPersistence implements ProjectionPersistencePort<RegisteredCustomers> {

    @Override
    public Snapshot<RegisteredCustomers> loadSnapshot() {
        return new Snapshot<>(new RegisteredCustomers(), 0);
    }

    @Override
    public void saveDelta(RegisteredCustomers delta, long newCheckpoint) {

    }
}
