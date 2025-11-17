package dev.ted.jitterticket.eventsourced.domain.customer;

import dev.ted.jitterticket.eventsourced.domain.Event;

public abstract sealed class CustomerEvent extends Event
        permits CustomerRegistered, TicketsPurchased {

    private final CustomerId customerId;

    protected CustomerEvent(CustomerId customerId,
                            Integer eventSequence) {
        super(eventSequence);
        this.customerId = customerId;
    }

    protected CustomerEvent(CustomerId customerId,
                            Integer eventSequence,
                            Long globalEventSequence) {
        super(eventSequence, globalEventSequence);
        this.customerId = customerId;
    }

    public CustomerId customerId() {
        return customerId;
    }
}
