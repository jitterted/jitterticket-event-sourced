package dev.ted.jitterticket.eventsourced.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        void registerCustomerGeneratesCustomerRegistered() {
            CustomerId customerId = new CustomerId(UUID.randomUUID());
            Customer customer = Customer.register(
                    customerId,
                    "customer name", "email@example.com");

            assertThat(customer.uncommittedEvents())
                    .containsExactly(
                            new CustomerRegistered(customerId, "customer name", "email@example.com")
                    );
        }
    }

    @Nested
    class EventsProjectState {

        @Test
        void customerRegisteredUpdatesNameAndEmail() {
            CustomerId customerId = new CustomerId(UUID.randomUUID());
            CustomerRegistered customerRegistered = new CustomerRegistered(
                    customerId, "customer name", "email@example.com");

            Customer customer = Customer.reconstitute(List.of(customerRegistered));

            assertThat(customer.getId())
                    .isEqualTo(customerId);
            assertThat(customer.name())
                    .isEqualTo("customer name");
            assertThat(customer.email())
                    .isEqualTo("email@example.com");
        }
    }

}