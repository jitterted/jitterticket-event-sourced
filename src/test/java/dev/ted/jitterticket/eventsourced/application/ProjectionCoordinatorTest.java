package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import static dev.ted.jitterticket.eventsourced.application.Assertions.assertThat;

class ProjectionCoordinatorTest {

    private static final String IRRELEVANT_EMAIL = "existing@example.com";

    @Test
    void projectionIsEmptyWhenNoCustomersAreRegistered() {
        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                new MemoryRegisteredCustomersProjectionPersistence(),
                InMemoryEventStore.forCustomers()
        );

        assertThat(projectionCoordinator.projection().asList())
                .isEmpty();
    }

    @Test
    void projectorCatchesUpForExistingCustomerRegisteredEvents() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(existingCustomerId, "Existing Customer", IRRELEVANT_EMAIL));
        var registeredCustomersProjector = new RegisteredCustomersProjector();

        var projectionCoordinator = new ProjectionCoordinator<>(
                registeredCustomersProjector,
                new MemoryRegisteredCustomersProjectionPersistence(),
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .containsExactly(
                        new RegisteredCustomers.RegisteredCustomer(existingCustomerId, "Existing Customer")
                );
    }

    @Test
    void newlySavedCustomerEventsUpdatesProjector() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId firstCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(firstCustomerId, "First Customer", "first@example.com"));
        var registeredCustomersProjector = new RegisteredCustomersProjector();
        var projectionCoordinator = new ProjectionCoordinator<>(
                registeredCustomersProjector,
                new MemoryRegisteredCustomersProjectionPersistence(),
                customerStore
        );

        CustomerId secondCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(secondCustomerId, "Second Customer", "second@example.com"));

        RegisteredCustomers.RegisteredCustomer firstRegisteredCustomer = new RegisteredCustomers.RegisteredCustomer(firstCustomerId, "First Customer");
        RegisteredCustomers.RegisteredCustomer secondRegisteredCustomer = new RegisteredCustomers.RegisteredCustomer(secondCustomerId, "Second Customer");
        assertThat(projectionCoordinator.projection().asList())
                .containsExactlyInAnyOrder(
                        firstRegisteredCustomer,
                        secondRegisteredCustomer
                );

        CustomerId thirdCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(thirdCustomerId, "Third Customer", "third@example.com"));

        RegisteredCustomers.RegisteredCustomer thirdRegisteredCustomer = new RegisteredCustomers.RegisteredCustomer(thirdCustomerId, "Third Customer");
        assertThat(projectionCoordinator.projection().asList())
                .containsExactlyInAnyOrder(
                        firstRegisteredCustomer,
                        secondRegisteredCustomer,
                        thirdRegisteredCustomer
                );
    }

    @Test
    void projectionLoadsNonEmptySnapshotUponCreation() {
        MemoryRegisteredCustomersProjectionPersistence projectionPersistence =
                new MemoryRegisteredCustomersProjectionPersistence();
        RegisteredCustomers delta = new RegisteredCustomers(
                new RegisteredCustomers.RegisteredCustomer(
                        CustomerId.createRandom(),
                        "Snapshotted Customer"));
        projectionPersistence.saveDelta(
                delta,
                Checkpoint.of(1));

        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                InMemoryEventStore.forCustomers()
        );

        assertThat(projectionCoordinator.projection())
                .isEqualTo(delta);
    }

    @Test
    void projectionDoesNotApplyEventsAlreadyProcessedInSnapshot() {
        MemoryRegisteredCustomersProjectionPersistence projectionPersistence =
                new MemoryRegisteredCustomersProjectionPersistence();
        CustomerId snapshottedCustomerId = CustomerId.createRandom();
        String snapshottedCustomerName = "Snapshotted Customer";
        RegisteredCustomers delta = new RegisteredCustomers(
                new RegisteredCustomers.RegisteredCustomer(
                        snapshottedCustomerId,
                        snapshottedCustomerName));
        projectionPersistence.saveDelta(
                delta,
                Checkpoint.of(1));

        var customerEventStore = InMemoryEventStore.forCustomers();
        customerEventStore.save(Customer.register(snapshottedCustomerId,
                                                  snapshottedCustomerName,
                                                  "dontcare@example.com"));

        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerEventStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .as("Projection should only contain the data loaded from the snapshot persistence")
                .hasSize(1);

    }
}
