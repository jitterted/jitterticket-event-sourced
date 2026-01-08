package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class RegisteredCustomersProjectorTest {

    private static final RegisteredCustomers EMPTY_REGISTERED_CUSTOMERS_STATE = new RegisteredCustomers();
    private static final String IRRELEVANT_EMAIL = "customer@example.com";

    @Test
    void projectReturnsEmptyNewStateWhenNoCustomersAreRegistered() {
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector();

        DomainProjector.ProjectorResult<RegisteredCustomers> projection =
                registeredCustomersProjector.project(EMPTY_REGISTERED_CUSTOMERS_STATE,
                                                     Stream.empty());

        assertThat(projection.fullState().asList())
                .isEmpty();
        assertThat(projection.delta().asList())
                .isEmpty();
    }

    @Test
    void fromEmptyStateProjectReturnsStateWithDeltaWithRegisteredCustomer() {
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector();
        CustomerId customerId = CustomerId.createRandom();
        CustomerEvent customerRegistered = new CustomerRegistered(
                customerId, 1L, "Customer Name", IRRELEVANT_EMAIL);
        var projection = registeredCustomersProjector.project(
                EMPTY_REGISTERED_CUSTOMERS_STATE,
                Stream.of(customerRegistered));

        var registeredCustomer = new RegisteredCustomers.RegisteredCustomer(
                customerId, "Customer Name");
        assertThat(projection.fullState().asList())
                .as("Expected the Projection Full State to have only the newly registered customer")
                .containsExactly(registeredCustomer);
        assertThat(projection.delta().asList())
                .as("Expected the Projection DELTA State to have only the newly registered customer")
                .containsExactly(registeredCustomer);
    }

    @Test
    void nonEmptyProjectionNoNewEventsReturnsSameFullStateAndEmptyDelta() {
        RegisteredCustomers nonEmptyCurrentProjectionState = new RegisteredCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        nonEmptyCurrentProjectionState.add(new RegisteredCustomers.RegisteredCustomer(
                existingCustomerId, "Existing Customer"));
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector();

        var projection = registeredCustomersProjector.project(
                nonEmptyCurrentProjectionState,
                Stream.empty());

        assertThat(projection.fullState())
                .as("New Projection state should be the same as the previous state")
                .isEqualTo(nonEmptyCurrentProjectionState);
        assertThat(projection.delta().hasData())
                .isFalse();
    }

    @Test
    void nonEmptyPreviousFullStateWithNewCustomerRegisteredReturnsFullStateAndDeltaWithNewRegistration() {
        RegisteredCustomers nonEmptyCurrentProjectionState = new RegisteredCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        nonEmptyCurrentProjectionState.add(new RegisteredCustomers.RegisteredCustomer(
                existingCustomerId, "Existing Customer"));
        CustomerId newCustomerId = CustomerId.createRandom();
        CustomerEvent customerRegistered = new CustomerRegistered(
                newCustomerId, 2L, "New Customer", IRRELEVANT_EMAIL);
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector();

        var projection = registeredCustomersProjector.project(
                nonEmptyCurrentProjectionState,
                Stream.of(customerRegistered));

        assertThat(projection.fullState().asList())
                .extracting(RegisteredCustomers.RegisteredCustomer::customerId)
                .containsExactly(existingCustomerId, newCustomerId);
        assertThat(projection.delta().asList())
                .extracting(RegisteredCustomers.RegisteredCustomer::customerId)
                .containsExactly(newCustomerId);
    }

}