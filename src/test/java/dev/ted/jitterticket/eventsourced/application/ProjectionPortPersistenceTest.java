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
                .hasCheckpointOf(0)
                .hasEmptyState();
    }

    @Test
    void savedDeltaWithEmptyDeltaBumpsCheckpoint() {
        // this is the situation where a projector saw new events, but its state didn't change (non-relevant events)
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();

        projectionPersistence.saveDelta(new RegisteredCustomers(),
                                        1);

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointOf(1)
                .hasEmptyState();
    }

    @Test
    void savedDeltaWithNewDataToEmptyPersistenceIsReturnedInSnapshot() {
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();

        RegisteredCustomers delta = createDeltaWith(CustomerId.createRandom());
        projectionPersistence.saveDelta(delta,
                                        1);

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointOf(1)
                .state()
                .isEqualTo(delta);
    }

    @Test
    void savedDeltaIsMergedWithExistingState() {
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();
        projectionPersistence.saveDelta(createDeltaWith("Existing Customer"), 1);

        RegisteredCustomers delta = createDeltaWith("New Customer");
        projectionPersistence.saveDelta(delta, 2);

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointOf(2);

        assertThat(snapshot.state().asList())
                .extracting(RegisteredCustomers.RegisteredCustomer::name)
                .contains("Existing Customer", "New Customer");
    }

    // test to ensure saveDelta has valid newCheckpoint, i.e., must be:
    // 1 or more, and
    // greater than existing checkpoint


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