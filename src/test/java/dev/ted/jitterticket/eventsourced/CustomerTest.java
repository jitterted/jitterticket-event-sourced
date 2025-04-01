package dev.ted.jitterticket.eventsourced;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CustomerTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        @Disabled("dev.ted.jitterticket.eventsourced.CustomerTest.CommandsGenerateEvents 4/1/25 10:36 — until we have a base class for event-sourced aggregates")
        void registerCustomerGeneratesCustomerRegistered() {
            Customer customer = Customer.register("name", "email@example.com");

            assertThat(customer)
                    .isNotNull();
        }
    }

    @Nested
    class EventsProjectState {
        
    }

}