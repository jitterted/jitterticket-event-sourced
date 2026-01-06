package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RegisteredCustomersProjectorTest {

    @Test
    void allCustomersReturnsEmptyStreamWhenNoCustomersAreRegistered() {
        var customerStore = InMemoryEventStore.forCustomers();
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector(customerStore);

        assertThat(registeredCustomersProjector.allCustomers().asList())
                .isEmpty();
    }

    @Test
    void projectorCollectsExistingCustomerRegisteredEvents() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId existingCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(existingCustomerId, "John Doe", "john.doe@example.com"));

        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector(customerStore);

        assertThat(registeredCustomersProjector.allCustomers().asList())
                .containsExactly(
                        new RegisteredCustomers.RegisteredCustomer(existingCustomerId, "John Doe")
                );
    }

    @Test
    void newlySavedCustomersUpdatesProjector() {
        var customerStore = InMemoryEventStore.forCustomers();
        CustomerId firstCustomerId = CustomerId.createRandom();
        customerStore.save(Customer.register(firstCustomerId, "First Customer", "john.doe@example.com"));
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector(customerStore);

        CustomerId secondCustomerId = CustomerId.createRandom();
        CustomerId thirdCustomerId = CustomerId.createRandom();

        customerStore.save(Customer.register(secondCustomerId, "Second Customer", "jane.smith@example.com"));
        customerStore.save(Customer.register(thirdCustomerId, "Third Customer", "bob.wilson@example.com"));

        assertThat(registeredCustomersProjector.allCustomers().asList())
                .containsExactlyInAnyOrder(
                        new RegisteredCustomers.RegisteredCustomer(firstCustomerId, "First Customer"),
                        new RegisteredCustomers.RegisteredCustomer(secondCustomerId, "Second Customer"),
                        new RegisteredCustomers.RegisteredCustomer(thirdCustomerId, "Third Customer")
                );
    }
}