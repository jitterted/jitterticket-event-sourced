package dev.ted.jitterticket.eventsourced.domain.customer;

public record CustomerRegistered(CustomerId customerId,
                                 Long eventSequence, String customerName,
                                 String email)
        implements CustomerEvent {
}
