package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import static dev.ted.jitterticket.eventsourced.application.Assertions.assertThat;

class ProjectionPortPersistenceTest {

    @Test
    void emptyPersistenceLoadsEmptySnapshotWithCheckpointAtZero() {
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointValueOf(0)
                .hasEmptyState();
    }

    @Test
    void savedDeltaWithEmptyDeltaBumpsCheckpoint() {
        // this is the situation where a projector saw new events, but its state didn't change (non-relevant events)
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();

        projectionPersistence.saveDelta(new RegisteredCustomers(),
                                        Checkpoint.of(1));

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointValueOf(1)
                .hasEmptyState();
    }

    @Test
    void savedDeltaWithNewDataToEmptyPersistenceIsReturnedInSnapshot() {
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();

        RegisteredCustomers delta = createDeltaWith(CustomerId.createRandom());
        projectionPersistence.saveDelta(delta,
                                        Checkpoint.of(1));

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointValueOf(1)
                .state()
                .isEqualTo(delta);
    }

    @Test
    void savedDeltaIsMergedWithExistingState() {
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();
        projectionPersistence.saveDelta(createDeltaWith("Existing Customer"), Checkpoint.of(1));

        RegisteredCustomers delta = createDeltaWith("New Customer");
        projectionPersistence.saveDelta(delta, Checkpoint.of(2));

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointValueOf(2);

        assertThat(snapshot.state().asList())
                .extracting(RegisteredCustomers.RegisteredCustomer::name)
                .contains("Existing Customer", "New Customer");
    }

    static RegisteredCustomers createDeltaWith(CustomerId customerId) {
        return createDeltaWith(customerId, "Customer Name");
    }

    static RegisteredCustomers createDeltaWith(String customerName) {
        return createDeltaWith(CustomerId.createRandom(), customerName);
    }

    static RegisteredCustomers createDeltaWith(CustomerId customerId, String customerName) {
        return new RegisteredCustomers(
                new RegisteredCustomers.RegisteredCustomer(
                        customerId,
                        customerName
                ));
    }
}