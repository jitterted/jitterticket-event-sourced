package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import static dev.ted.jitterticket.eventsourced.application.Assertions.assertThat;

class ProjectionPersistencePortTest {

    @Test
    void emptyPersistenceLoadsProjectorWithEmptyStateAndCheckpointAtInitial() {
        var projectionPersistence = new NewMemoryRegisteredCustomersProjectionPersistence();

        var projector = projectionPersistence.loadProjector();

        assertThat(projector.projection())
                .hasCheckpointValueOf(0)
                .hasEmptyState();
    }

    @Test
    void savedDeltaWithEmptyDeltaBumpsCheckpoint() {
        // this is the situation where a projector saw new events, but its state didn't change (non-relevant events)
        var projectionPersistence =
                new NewMemoryRegisteredCustomersProjectionPersistence();

        projectionPersistence.saveDelta(
                new Checkpointed<>(new NewlyRegisteredCustomers(), Checkpoint.of(1)));

        var projector = projectionPersistence.loadProjector();

        assertThat(projector.projection())
                .hasCheckpointValueOf(1)
                .hasEmptyState();
    }

    @Test
    void deltaWithNewDataSavedToEmptyPersistenceIsReturnedInProjector() {
        var projectionPersistence =
                new NewMemoryRegisteredCustomersProjectionPersistence();

        RegisteredCustomer registeredCustomer = new RegisteredCustomer(CustomerId.createRandom(), "Customer Name");
        NewlyRegisteredCustomers delta = NewlyRegisteredCustomers
                .createForTestWith(registeredCustomer);
        projectionPersistence.saveDelta(new Checkpointed<>(delta, Checkpoint.of(1)));

        var projector = projectionPersistence.loadProjector();

        assertThat(projector.projection())
                .hasCheckpointValueOf(1)
                .state()
                .isEqualTo(AllRegisteredCustomers.of(registeredCustomer));
    }

    @Test
    void savedDeltaIsMergedWithExistingState() {
        var projectionPersistence = new NewMemoryRegisteredCustomersProjectionPersistence();
        Checkpoint checkpoint = Checkpoint.of(1);
        projectionPersistence.saveDelta(
                new Checkpointed<>(createDeltaWith("Existing Customer"
                ), checkpoint));

        Checkpoint deltaCheckpoint = Checkpoint.of(2);
        NewlyRegisteredCustomers delta = createDeltaWith("New Customer"
        );
        projectionPersistence.saveDelta(new Checkpointed<>(delta, deltaCheckpoint));

        var projector = projectionPersistence.loadProjector();

        assertThat(projector.projection())
                .hasCheckpointValueOf(2);

        assertThat(projector.projection().state().asList())
                .extracting(RegisteredCustomer::name)
                .contains("Existing Customer", "New Customer");
    }

    static NewlyRegisteredCustomers createDeltaWith(String customerName) {
        return NewlyRegisteredCustomers.createForTestWith(
                new RegisteredCustomer(
                        CustomerId.createRandom(),
                        customerName
                ));
    }

}