package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class NewRegisteredCustomersProjectorTest {

    private static final String IRRELEVANT_EMAIL = "customer@example.com";

    @Test
    void emptyStateAndCheckpointUnchangedWhenNoEventsAreHandled() {
        NewRegisteredCustomersProjector registeredCustomersProjector =
                NewRegisteredCustomersProjector.createEmpty();

        assertThat(registeredCustomersProjector.projection().state().asList())
                .isEmpty();
        assertThat(registeredCustomersProjector.flush().state().asList())
                .isEmpty();
        assertThat(registeredCustomersProjector.flush().checkpoint())
                .isEqualTo(Checkpoint.INITIAL);
    }

    @Test
    void fromEmptyStateUpdatesStateAfterHandlingRegisteredCustomer() {
        NewRegisteredCustomersProjector registeredCustomersProjector =
                NewRegisteredCustomersProjector.createEmpty();
        CustomerId customerId = CustomerId.createRandom();
        long eventSequence = 1L;
        CustomerEvent customerRegistered = new CustomerRegistered(
                customerId, eventSequence, "Customer Name", IRRELEVANT_EMAIL);
        registeredCustomersProjector.handle(Stream.of(customerRegistered));

        var registeredCustomer = new RegisteredCustomer(
                customerId, "Customer Name");
        assertThat(registeredCustomersProjector.projection().state().asList())
                .as("Expected the Projection Full State to have only the newly registered customer")
                .containsExactly(registeredCustomer);
        Checkpointed<NewlyRegisteredCustomers> registeredCustomers = registeredCustomersProjector.flush();
        assertThat(registeredCustomers.state().asList())
                .as("Expected the Projection DELTA State to have only the newly registered customer")
                .containsExactly(registeredCustomer);
        assertThat(registeredCustomers.checkpoint())
                .isEqualTo(Checkpoint.of(eventSequence));
    }

    @Test
    void nonEmptyPreviousFullStateWithNewCustomerRegisteredReturnsFullStateAndDeltaWithNewRegistration() {
        Fixture fixture = createStateWithCustomerRegistered();
        CustomerId newCustomerId = CustomerId.createRandom();
        long eventSequence = 2L;
        CustomerRegistered customerRegistered = new CustomerRegistered(
                newCustomerId, eventSequence, "Second Customer", IRRELEVANT_EMAIL);
        NewRegisteredCustomersProjector registeredCustomersProjector =
                new NewRegisteredCustomersProjector(
                        fixture.nonEmptyCurrentProjectionState);

        registeredCustomersProjector.handle(customerRegistered);

        assertThat(registeredCustomersProjector.projection().state().asList())
                .as("Expected Full Current State to have 2 Customers")
                .extracting(RegisteredCustomer::customerId)
                .containsExactly(fixture.existingCustomerId(), newCustomerId);
        Checkpointed<NewlyRegisteredCustomers> registeredCustomersDelta = registeredCustomersProjector.flush();
        assertThat(registeredCustomersDelta.state().asList())
                .as("Expecting Delta to have 1 New Customer")
                .extracting(RegisteredCustomer::customerId)
                .containsExactly(newCustomerId);
        assertThat(registeredCustomersDelta.checkpoint())
                .isEqualTo(Checkpoint.of(eventSequence));
    }

    @Test
    void projectorRetainsMultipleUncommittedChangesUntilFlushed() {
        NewRegisteredCustomersProjector registeredCustomersProjector =
                new NewRegisteredCustomersProjector(createStateWithCustomerRegistered().nonEmptyCurrentProjectionState);

        registeredCustomersProjector.handle(new CustomerRegistered(CustomerId.createRandom(), 1L,
                                                                   "First Customer", IRRELEVANT_EMAIL));
        registeredCustomersProjector.handle(new CustomerRegistered(CustomerId.createRandom(), 2L,
                                                                   "Second Customer", IRRELEVANT_EMAIL));
        long lastEventSequenceProcessed = 3L;
        registeredCustomersProjector.handle(new CustomerRegistered(CustomerId.createRandom(), lastEventSequenceProcessed,
                                                                   "Third Customer", IRRELEVANT_EMAIL));

        Checkpointed<NewlyRegisteredCustomers> registeredCustomersDelta1 = registeredCustomersProjector.flush();
        assertThat(registeredCustomersDelta1.state().asList())
                .extracting(RegisteredCustomer::name)
                .containsExactly("First Customer", "Second Customer", "Third Customer");
        assertThat(registeredCustomersDelta1.checkpoint())
                .as("From first flush(), Checkpoint should be 3 after processing through event sequence #3")
                .isEqualTo(Checkpoint.of(lastEventSequenceProcessed));

        Checkpointed<NewlyRegisteredCustomers> registeredCustomersDelta2 = registeredCustomersProjector.flush();
        assertThat(registeredCustomersDelta2.state().isEmpty())
                .as("Flush should have emptied the uncommitted changes")
                .isTrue();
        assertThat(registeredCustomersDelta2.checkpoint())
                .as("No new events processed since previous flush(), so Checkpoint should still be 3")
                .isEqualTo(Checkpoint.of(lastEventSequenceProcessed));
    }

    private static Fixture createStateWithCustomerRegistered() {
        AllRegisteredCustomers nonEmptyCurrentProjectionState =
                new AllRegisteredCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        nonEmptyCurrentProjectionState.add(
                new RegisteredCustomer(existingCustomerId, "Existing Customer"));
        return new Fixture(nonEmptyCurrentProjectionState, existingCustomerId);
    }

    private record Fixture(AllRegisteredCustomers nonEmptyCurrentProjectionState,
                           CustomerId existingCustomerId) {}

}