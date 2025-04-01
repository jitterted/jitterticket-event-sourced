package dev.ted.jitterticket.eventsourced.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EventSourcedAggregateTest {

    @Test
    void eventsAreAppliedUponBeingEnqueued() {
        var eventSourcedAggregate = new EventSourcedAggregate<String, Long>() {
            private String appliedString;

            @Override
            protected void apply(String s) {
                this.appliedString = s;
            }
        };

        eventSourcedAggregate.enqueue("StringlyEvent");

        assertThat(eventSourcedAggregate.appliedString)
                .isEqualTo("StringlyEvent");
    }
}