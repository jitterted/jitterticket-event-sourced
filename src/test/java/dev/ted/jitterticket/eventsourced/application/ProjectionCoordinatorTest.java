package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

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

        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                new MemoryRegisteredCustomersProjectionPersistence(),
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .containsExactly(
                        new RegisteredCustomers.RegisteredCustomer(existingCustomerId, "Existing Customer")
                );
    }

    @Test
    void projectorStateSameAfterCatchingUpForNoNewEvents() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(
                existingCustomerId, "Existing Customer", IRRELEVANT_EMAIL));
        // catch up on new customer registered
        MemoryRegisteredCustomersProjectionPersistence projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();
        new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerStore
        );

        // this won't have any catching up to do, as no new events since last catch-up
        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .containsExactly(
                        new RegisteredCustomers.RegisteredCustomer(existingCustomerId, "Existing Customer")
                );
        assertThat(projectionPersistence.loadSnapshot().checkpoint())
                .isEqualTo(Checkpoint.of(1L));
    }

    @Test
    void handleEmptyStreamPreservesPersistedCheckpoint() {
        MemoryRegisteredCustomersProjectionPersistence projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId customerId = CustomerId.createRandom();
        customerStore.save(Customer.register(customerId, "Existing Customer", IRRELEVANT_EMAIL));
        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerStore
        );

        projectionCoordinator.handle(Stream.empty());

        assertThat(projectionPersistence.loadSnapshot().checkpoint())
                .as("Checkpoint should still be 1 (unchanged) after handling an empty stream")
                .isEqualTo(Checkpoint.of(1L));
    }

    @Test
    void doesNotPersistIfNewProjectionWithCheckpointIsUnchanged() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(existingCustomerId, "Existing Customer", IRRELEVANT_EMAIL));
        ProjectionPersistencePort<RegisteredCustomers> projectionPersistence = new ProjectionPersistencePort<RegisteredCustomers>() {
            @Override
            public Snapshot<RegisteredCustomers> loadSnapshot() {
                return new Snapshot<>(new RegisteredCustomers(
                        new RegisteredCustomers.RegisteredCustomer(
                                existingCustomerId, "Existing Customer")
                ), Checkpoint.of(1));
            }

            @Override
            public void saveDelta(RegisteredCustomers delta, Checkpoint newCheckpoint) {
                throw new IllegalStateException(
                        "Should not have attempted to persist projection delta: "
                        + delta.asList()
                        + ", new checkpoint = "
                        + newCheckpoint);
            }
        };
        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .hasSize(1);
    }

    @Test
    void newlySavedCustomerEventsUpdatesProjector() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId firstCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(firstCustomerId, "First Customer", "first@example.com"));
        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
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
                                                  IRRELEVANT_EMAIL));

        var projectionCoordinator = new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerEventStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .as("Projection should only contain the data loaded from the snapshot persistence")
                .hasSize(1);
    }

    @Test
    void projectionPersistedAfterUpdatedAfterCatchUp() {
        MemoryRegisteredCustomersProjectionPersistence projectionPersistence =
                new MemoryRegisteredCustomersProjectionPersistence();
        var customerEventStore = InMemoryEventStore.forCustomers();
        CustomerId customerId = CustomerId.createRandom();
        String customerName = "Catchup Customer";
        customerEventStore.save(Customer.register(customerId,
                                                  customerName,
                                                  IRRELEVANT_EMAIL));

        new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerEventStore
        );

        assertThat(projectionPersistence.loadSnapshot().checkpoint())
                .as("Checkpoint should be 1 after catching up on a single CustomerRegistered event in the event store")
                .isEqualTo(Checkpoint.of(1L));
        assertThat(projectionPersistence.loadSnapshot().state().asList())
                .containsExactly(new RegisteredCustomers
                        .RegisteredCustomer(customerId, customerName));
    }

    @Test
    void projectionPersistedAfterUpdatedViaHandlingSingleNewEvent() {
        Fixture fixture = createEmptyProjectionCoordinator();

        RegisteredCustomers.RegisteredCustomer expectedRegisteredCustomer =
                saveNewlyRegisteredCustomer(fixture, "New Customer");

        assertThat(fixture.projectionPersistence.loadSnapshot().checkpoint())
                .as("Checkpoint should be 1 after handling a single CustomerRegistered event from the event store")
                .isEqualTo(Checkpoint.of(1L));
        assertThat(fixture.projectionPersistence.loadSnapshot().state().asList())
                .containsExactly(expectedRegisteredCustomer);
    }

    @Test
    void persistedProjectionUpdatedAfterHandlingMultipleNewEvents() {
        Fixture fixture = createEmptyProjectionCoordinator();

        RegisteredCustomers.RegisteredCustomer firstRegisteredCustomer =
                saveNewlyRegisteredCustomer(fixture, "First New Customer");
        RegisteredCustomers.RegisteredCustomer secondRegisteredCustomer =
                saveNewlyRegisteredCustomer(fixture, "Second New Customer");

        assertThat(fixture.projectionPersistence.loadSnapshot().checkpoint())
                .as("Checkpoint should be 2 after handling two CustomerRegistered events from the event store")
                .isEqualTo(Checkpoint.of(2L));
        assertThat(fixture.projectionPersistence.loadSnapshot().state().asList())
                .containsExactlyInAnyOrder(firstRegisteredCustomer,
                                           secondRegisteredCustomer);
    }

    private static RegisteredCustomers.RegisteredCustomer saveNewlyRegisteredCustomer(Fixture fixture, String customerName) {
        CustomerId customerId = CustomerId.createRandom();
        fixture.customerEventStore.save(Customer.register(customerId,
                                                          customerName,
                                                          IRRELEVANT_EMAIL));
        return new RegisteredCustomers.RegisteredCustomer(
                customerId, customerName);
    }

    private static Fixture createEmptyProjectionCoordinator() {
        var customerEventStore = InMemoryEventStore.forCustomers();
        var projectionPersistence = new MemoryRegisteredCustomersProjectionPersistence();
        new ProjectionCoordinator<>(
                new RegisteredCustomersProjector(),
                projectionPersistence,
                customerEventStore
        );
        return new Fixture(customerEventStore, projectionPersistence);
    }

    private record Fixture(
            dev.ted.jitterticket.eventsourced.application.port.EventStore<CustomerId, dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent, Customer> customerEventStore,
            MemoryRegisteredCustomersProjectionPersistence projectionPersistence) {}
}
