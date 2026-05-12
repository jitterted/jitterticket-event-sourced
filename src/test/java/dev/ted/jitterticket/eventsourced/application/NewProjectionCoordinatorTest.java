package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static dev.ted.jitterticket.eventsourced.application.Assertions.assertThat;

class NewProjectionCoordinatorTest {

    private static final String IRRELEVANT_EMAIL = "existing@example.com";

    @Test
    void projectionIsEmptyWhenNoCustomersAreRegistered() {
        var projectionCoordinator = new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                new NewMemoryRegisteredCustomersProjectionPersistence(),
                InMemoryEventStore.forCustomers()
        );

        assertThat(projectionCoordinator.projection().asList())
                .isEmpty();
    }

    @Test
    void projectionLoadsNonEmptySnapshotUponCreation() {
        NewMemoryRegisteredCustomersProjectionPersistence projectionPersistence =
                new NewMemoryRegisteredCustomersProjectionPersistence();
        NewlyRegisteredCustomers delta = NewlyRegisteredCustomers.createForTestWith(
                Checkpoint.of(1),
                new RegisteredCustomer(
                        CustomerId.createRandom(),
                        "Snapshotted Customer")
                );
        projectionPersistence.saveDelta(
                new Checkpointed<>(delta, Checkpoint.of(1)));

        var projectionCoordinator = new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                projectionPersistence,
                InMemoryEventStore.forCustomers()
        );

        assertThat(projectionCoordinator.projection().asList())
                .isEqualTo(delta.asList());
    }

    @Nested
    class ProjectionPersistence {

        @Test
        void projectionPersistedAfterUpdatedViaHandlingSingleNewEvent() {
            Fixture fixture = createEmptyProjectionCoordinator();

            RegisteredCustomer expectedRegisteredCustomer =
                    saveViaCustomerRegisteredEvent(fixture, "New Customer");

            var postSaveSnapshot =
                    fixture.projectionPersistence.loadSnapshot();
            assertThat(postSaveSnapshot.checkpoint())
                    .as("Checkpoint should be 1 after handling a single CustomerRegistered event from the event store")
                    .isEqualTo(Checkpoint.of(1L));
            assertThat(postSaveSnapshot.state().asList())
                    .containsExactly(expectedRegisteredCustomer);
        }

        @Test
        void persistedProjectionUpdatedAfterHandlingMultipleNewEvents() {
            Fixture fixture = createEmptyProjectionCoordinator();

            RegisteredCustomer firstRegisteredCustomer =
                    saveViaCustomerRegisteredEvent(fixture, "First New Customer");
            RegisteredCustomer secondRegisteredCustomer =
                    saveViaCustomerRegisteredEvent(fixture, "Second New Customer");

            var snapshot = fixture.projectionPersistence.loadSnapshot();
            assertThat(snapshot.checkpoint())
                    .as("Checkpoint should be 2 after handling two CustomerRegistered events from the event store")
                    .isEqualTo(Checkpoint.of(2L));
            assertThat(snapshot.state().asList())
                    .containsExactlyInAnyOrder(firstRegisteredCustomer,
                                               secondRegisteredCustomer);
        }
    }

    @Test
    void projectorCatchesUpForExistingCustomerRegisteredEvents() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(existingCustomerId, "Existing Customer", IRRELEVANT_EMAIL));

        var projectionCoordinator = new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                new NewMemoryRegisteredCustomersProjectionPersistence(),
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .containsExactly(
                        new RegisteredCustomer(existingCustomerId, "Existing Customer")
                );
    }

    @Disabled("Until Projection Snapshot Loading works")
    @Test
    void projectorStateSameAfterCatchingUpForNoNewEvents() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(
                existingCustomerId, "Existing Customer", IRRELEVANT_EMAIL));
        // catch up on new customer registered
        NewMemoryRegisteredCustomersProjectionPersistence projectionPersistence = new NewMemoryRegisteredCustomersProjectionPersistence();
        new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                projectionPersistence,
                customerStore
        );

        // this won't have any catching up to do, as no new events since last catch-up
        var projectionCoordinator = new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                projectionPersistence,
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .containsExactly(
                        new RegisteredCustomer(existingCustomerId, "Existing Customer")
                );
        assertThat(projectionPersistence.loadSnapshot().checkpoint())
                .isEqualTo(Checkpoint.of(1L));
    }

//    @Test
    void handleEmptyStreamPreservesPersistedCheckpoint() {
        var projectionPersistence = new ConfigurableCrashingProjectionPersistence(1);
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId customerId = CustomerId.createRandom();
        var projectionCoordinator = new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                projectionPersistence,
                customerStore
        );
        // cachedCheckpoint = 0, persistedCheckpoint = 0
        customerStore.save(Customer.register(customerId, "Existing Customer", IRRELEVANT_EMAIL));
        // cachedCheckpoint = 0, persistedCheckpoint = 1

        projectionCoordinator.handle(Stream.empty());
        // cachedCheckpoint = 0, persistedCheckpoint = 1

        assertThat(projectionPersistence.loadSnapshot().checkpoint())
                .as("Checkpoint should still be 1 (unchanged) after handling an empty stream")
                .isEqualTo(Checkpoint.of(1L));
    }

//    @Test
    void doesNotPersistIfNewProjectionWithCheckpointIsUnchanged() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(existingCustomerId, "Existing Customer", IRRELEVANT_EMAIL));
        var projectionCoordinator = new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                new NewMemoryRegisteredCustomersProjectionPersistence(),
                customerStore
        );

        assertThat(projectionCoordinator.projection().asList())
                .hasSize(1);
    }

//    @Test
//    void inconsistentEventStreamsCauseCheckpointRegressionIfCachedCheckpointIsNotUpdated() {
//        var customerEventStore = InMemoryEventStore.forCustomers();
//        var projectionPersistence = new NewMemoryRegisteredCustomersProjectionPersistence();
//        var projectionCoordinator = new NewProjectionCoordinator<>(
//                NewRegisteredCustomersProjector.createEmpty(),
//                projectionPersistence,
//                customerEventStore
//        );
//
//        customerEventStore.save(Customer.register(
//                CustomerId.createRandom(), "First Customer", "first@example.com"));
//        customerEventStore.save(Customer.register(
//                CustomerId.createRandom(), "Second Customer", "second@example.com"));
//
//        // Simulate an "out of order" or "delayed" event with a "re-handle" of event 1
//        // grabbing it from the event store ensure it has the event sequence assigned
//        var event1 = customerEventStore.allEventsAfter(Checkpoint.INITIAL)
//                                       .findFirst().orElseThrow();
//        projectionCoordinator.handle(Stream.of(event1));
//
//        assertThat(projectionPersistence.loadSnapshot().checkpoint())
//                .as("Checkpoint should not regress to 1 after it has reached 2")
//                .isEqualTo(Checkpoint.of(2L));
//    }
//
//    @Test
//    void newlySavedCustomerEventsUpdatesProjector() {
//        var customerStore = InMemoryEventStore.forCustomers();
//        CustomerId firstCustomerId = CustomerId.createRandom();
//        customerStore.save(Customer.register(firstCustomerId, "First Customer", "first@example.com"));
//        var projectionCoordinator = new NewProjectionCoordinator<>(
//                NewRegisteredCustomersProjector.createEmpty(),
//                new NewMemoryRegisteredCustomersProjectionPersistence(),
//                customerStore
//        );
//
//        CustomerId secondCustomerId = CustomerId.createRandom();
//        customerStore.save(Customer.register(secondCustomerId, "Second Customer", "second@example.com"));
//
//        RegisteredCustomers.RegisteredCustomer firstRegisteredCustomer = new RegisteredCustomers.RegisteredCustomer(firstCustomerId, "First Customer");
//        RegisteredCustomers.RegisteredCustomer secondRegisteredCustomer = new RegisteredCustomers.RegisteredCustomer(secondCustomerId, "Second Customer");
//        assertThat(projectionCoordinator.projection().asList())
//                .containsExactlyInAnyOrder(
//                        firstRegisteredCustomer,
//                        secondRegisteredCustomer
//                );
//
//        CustomerId thirdCustomerId = CustomerId.createRandom();
//        customerStore.save(Customer.register(thirdCustomerId, "Third Customer", "third@example.com"));
//
//        RegisteredCustomers.RegisteredCustomer thirdRegisteredCustomer = new RegisteredCustomers.RegisteredCustomer(thirdCustomerId, "Third Customer");
//        assertThat(projectionCoordinator.projection().asList())
//                .containsExactlyInAnyOrder(
//                        firstRegisteredCustomer,
//                        secondRegisteredCustomer,
//                        thirdRegisteredCustomer
//                );
//    }

//    @Test
//    void projectionDoesNotApplyEventsAlreadyProcessedInSnapshot() {
//        NewMemoryRegisteredCustomersProjectionPersistence projectionPersistence =
//                new MemoryRegisteredCustomersProjectionPersistence();
//        CustomerId snapshottedCustomerId = CustomerId.createRandom();
//        String snapshottedCustomerName = "Snapshotted Customer";
//        RegisteredCustomers delta = new RegisteredCustomers(
//                new RegisteredCustomers.RegisteredCustomer(
//                        snapshottedCustomerId,
//                        snapshottedCustomerName));
//        projectionPersistence.saveDelta(
//                delta,
//                Checkpoint.of(1));
//
//        var customerEventStore = InMemoryEventStore.forCustomers();
//        customerEventStore.save(Customer.register(snapshottedCustomerId,
//                                                  snapshottedCustomerName,
//                                                  IRRELEVANT_EMAIL));
//
//        var projectionCoordinator = new ProjectionCoordinator<>(
//                new RegisteredCustomersProjector(),
//                projectionPersistence,
//                customerEventStore
//        );
//
//        assertThat(projectionCoordinator.projection().asList())
//                .as("Projection should only contain the data loaded from the snapshot persistence")
//                .hasSize(1);
//    }
//
//    @Test
//    void projectionPersistedAfterUpdatedAfterCatchUp() {
//        NewMemoryRegisteredCustomersProjectionPersistence projectionPersistence =
//                new MemoryRegisteredCustomersProjectionPersistence();
//        var customerEventStore = InMemoryEventStore.forCustomers();
//        CustomerId customerId = CustomerId.createRandom();
//        String customerName = "Catchup Customer";
//        customerEventStore.save(Customer.register(customerId,
//                                                  customerName,
//                                                  IRRELEVANT_EMAIL));
//
//        new ProjectionCoordinator<>(
//                new RegisteredCustomersProjector(),
//                projectionPersistence,
//                customerEventStore
//        );
//
//        assertThat(projectionPersistence.loadSnapshot().checkpoint())
//                .as("Checkpoint should be 1 after catching up on a single CustomerRegistered event in the event store")
//                .isEqualTo(Checkpoint.of(1L));
//        assertThat(projectionPersistence.loadSnapshot().state().asList())
//                .containsExactly(new RegisteredCustomers
//                        .RegisteredCustomer(customerId, customerName));
//    }



    private static RegisteredCustomer saveViaCustomerRegisteredEvent(Fixture fixture, String customerName) {
        CustomerId customerId = CustomerId.createRandom();
        fixture.customerEventStore.save(Customer.register(customerId,
                                                          customerName,
                                                          IRRELEVANT_EMAIL));
        return new RegisteredCustomer(
                customerId, customerName);
    }

    private static Fixture createEmptyProjectionCoordinator() {
        var customerEventStore = InMemoryEventStore.forCustomers();
        var projectionPersistence = new NewMemoryRegisteredCustomersProjectionPersistence();
        new NewProjectionCoordinator<>(
                NewRegisteredCustomersProjector.createEmpty(),
                projectionPersistence,
                customerEventStore
        );
        return new Fixture(customerEventStore, projectionPersistence);
    }

    private record Fixture(
            EventStore<CustomerId, CustomerEvent, Customer> customerEventStore,
            NewMemoryRegisteredCustomersProjectionPersistence projectionPersistence) {}

    private static class ConfigurableCrashingProjectionPersistence extends NewMemoryRegisteredCustomersProjectionPersistence {

        private final int saveDeltaAttemptsBeforeCrash;
        private int saveDeltaAttemptCount = 0;

        public ConfigurableCrashingProjectionPersistence(int saveDeltaAttemptsBeforeCrash) {
            this.saveDeltaAttemptsBeforeCrash = saveDeltaAttemptsBeforeCrash;
        }

        @Override
        public void saveDelta(Checkpointed<NewlyRegisteredCustomers> checkpointed) {
            if (++saveDeltaAttemptCount > saveDeltaAttemptsBeforeCrash) {
                throw new IllegalStateException(
                        "Should not have attempted to persist projection delta: "
                        + checkpointed.state().asList()
                        + ", new checkpoint = "
                        + checkpointed.checkpoint());
            } else {
                super.saveDelta(checkpointed);
            }
        }
    }
}
