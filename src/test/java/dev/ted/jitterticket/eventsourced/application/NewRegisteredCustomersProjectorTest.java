package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class NewRegisteredCustomersProjectorTest {

    private static final RegisteredCustomers EMPTY_REGISTERED_CUSTOMERS_STATE = new RegisteredCustomers();
    private static final String IRRELEVANT_EMAIL = "customer@example.com";

    @Test
    void currentStateIsUnchangedWhenNoEventsAreHandled() {
        NewRegisteredCustomersProjector registeredCustomersProjector =
                new NewRegisteredCustomersProjector(
                        EMPTY_REGISTERED_CUSTOMERS_STATE);

        assertThat(registeredCustomersProjector.currentState().asList())
                .isEmpty();
        assertThat(registeredCustomersProjector.flush().asList())
                .isEmpty();
    }

    @Test
    void fromEmptyStateUpdatesStateAfterHandlingRegisteredCustomer() {
        NewRegisteredCustomersProjector registeredCustomersProjector =
                new NewRegisteredCustomersProjector(
                        EMPTY_REGISTERED_CUSTOMERS_STATE);
        CustomerId customerId = CustomerId.createRandom();
        CustomerEvent customerRegistered = new CustomerRegistered(
                customerId, 1L, "Customer Name", IRRELEVANT_EMAIL);
        registeredCustomersProjector.handle(Stream.of(customerRegistered));

        var registeredCustomer = new RegisteredCustomers.RegisteredCustomer(
                customerId, "Customer Name");
        assertThat(registeredCustomersProjector.currentState().asList())
                .as("Expected the Projection Full State to have only the newly registered customer")
                .containsExactly(registeredCustomer);
        assertThat(registeredCustomersProjector.flush().asList())
                .as("Expected the Projection DELTA State to have only the newly registered customer")
                .containsExactly(registeredCustomer);
    }

    @Test
    void nonEmptyPreviousFullStateWithNewCustomerRegisteredReturnsFullStateAndDeltaWithNewRegistration() {
        Fixture fixture = createStateWithCustomerRegistered();
        CustomerId newCustomerId = CustomerId.createRandom();
        CustomerRegistered customerRegistered = new CustomerRegistered(
                newCustomerId, 2L, "Second Customer", IRRELEVANT_EMAIL);
        NewRegisteredCustomersProjector registeredCustomersProjector =
                new NewRegisteredCustomersProjector(
                        fixture.nonEmptyCurrentProjectionState);

        registeredCustomersProjector.handle(customerRegistered);

        assertThat(registeredCustomersProjector.currentState().asList())
                .as("Expected Full Current State to have 2 Customers")
                .extracting(RegisteredCustomers.RegisteredCustomer::customerId)
                .containsExactly(fixture.existingCustomerId(), newCustomerId);
        assertThat(registeredCustomersProjector.flush().asList())
                .as("Expecting Delta to have 1 New Customer")
                .extracting(RegisteredCustomers.RegisteredCustomer::customerId)
                .containsExactly(newCustomerId);
    }

    @Test
    void projectorRetainsMultipleUncommittedChangesUntilFlushed() {
        NewRegisteredCustomersProjector registeredCustomersProjector =
                new NewRegisteredCustomersProjector(createStateWithCustomerRegistered().nonEmptyCurrentProjectionState);

        registeredCustomersProjector.handle(new CustomerRegistered(CustomerId.createRandom(), 1L,
                                                                   "First Customer", IRRELEVANT_EMAIL));
        registeredCustomersProjector.handle(new CustomerRegistered(CustomerId.createRandom(), 2L,
                                                                   "Second Customer", IRRELEVANT_EMAIL));
        registeredCustomersProjector.handle(new CustomerRegistered(CustomerId.createRandom(), 3L,
                                                                   "Third Customer", IRRELEVANT_EMAIL));

        assertThat(registeredCustomersProjector.flush().asList())
                .extracting(RegisteredCustomers.RegisteredCustomer::name)
                .containsExactly("First Customer", "Second Customer", "Third Customer");

        assertThat(registeredCustomersProjector.flush().isEmpty())
                .as("Flush should have emptied the uncommitted changes")
                .isTrue();
    }

    private static Fixture createStateWithCustomerRegistered() {
        RegisteredCustomers nonEmptyCurrentProjectionState =
                new RegisteredCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        nonEmptyCurrentProjectionState.add(new RegisteredCustomers.RegisteredCustomer(
                existingCustomerId, "Existing Customer"));
        return new Fixture(nonEmptyCurrentProjectionState, existingCustomerId);
    }

    private record Fixture(RegisteredCustomers nonEmptyCurrentProjectionState, CustomerId existingCustomerId) {}

}