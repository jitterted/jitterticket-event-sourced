package dev.ted.jitterticket.eventsourced.domain.customer;

public record CustomerRegistered(CustomerId customerId,
                                 Integer eventSequence,
                                 String customerName,
                                 String email)
        implements CustomerEvent {
}
