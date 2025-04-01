package dev.ted.jitterticket.eventsourced;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
        
    }

}