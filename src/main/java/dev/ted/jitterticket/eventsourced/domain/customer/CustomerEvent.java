package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.Event;

public sealed interface CustomerEvent extends Event
        permits CustomerRegistered {
    CustomerId customerId();
}
