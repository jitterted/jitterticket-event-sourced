package dev.ted.jitterticket.eventsourced.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        void registerCustomerGeneratesCustomerRegistered() {
            Customer customer = Customer.register("customer name", "email@example.com");

            assertThat(customer.uncommittedEvents())
                    .containsExactly(
                            new CustomerRegistered("customer name", "email@example.com")
                    );
        }
    }

    @Nested
    class EventsProjectState {

        @Test
        void customerRegisteredUpdatesNameAndEmail() {
            CustomerRegistered customerRegistered = new CustomerRegistered(
                    "customer name", "email@example.com");

            Customer customer = Customer.reconstitute(List.of(customerRegistered));

            assertThat(customer.name())
                    .isEqualTo("customer name");
            assertThat(customer.email())
                    .isEqualTo("email@example.com");
        }
    }

}