package dev.ted.jitterticket.eventsourced.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EventSourcedAggregateTest {

    @Test
    void eventsAreAppliedUponBeingEnqueued() {
        var eventSourcedAggregate = new EventSourcedAggregate<>() {
            private Event appliedEvent;

            @Override
            protected void apply(Event event) {
                appliedEvent = event;
            }
        };

        CustomerRegistered event = new CustomerRegistered("name", "email");
        eventSourcedAggregate.enqueue(event);

        assertThat(eventSourcedAggregate.appliedEvent)
                .isEqualTo(event);
    }

}
