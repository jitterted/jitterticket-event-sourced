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

        assertThat(registeredCustomersProjector.allCustomers())
                .isEmpty();
    }

    @Test
    void allCustomersReturnsCustomerSummariesForRegisteredCustomers() {
        var customerStore = InMemoryEventStore.forCustomers();
        RegisteredCustomersProjector registeredCustomersProjector =
                new RegisteredCustomersProjector(customerStore);

        CustomerId firstCustomerId = CustomerId.createRandom();
        CustomerId secondCustomerId = CustomerId.createRandom();
        CustomerId thirdCustomerId = CustomerId.createRandom();

        customerStore.save(Customer.register(firstCustomerId, "John Doe", "john.doe@example.com"));
        customerStore.save(Customer.register(secondCustomerId, "Jane Smith", "jane.smith@example.com"));
        customerStore.save(Customer.register(thirdCustomerId, "Bob Wilson", "bob.wilson@example.com"));

        assertThat(registeredCustomersProjector.allCustomers())
                .containsExactlyInAnyOrder(
                        new CustomerSummary(firstCustomerId, "John Doe"),
                        new CustomerSummary(secondCustomerId, "Jane Smith"),
                        new CustomerSummary(thirdCustomerId, "Bob Wilson")
                );
    }
}