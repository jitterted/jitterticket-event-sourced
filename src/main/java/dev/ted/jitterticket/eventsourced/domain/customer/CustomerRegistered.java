package dev.ted.jitterticket.eventsourced.domain.customer;

public record CustomerRegistered(CustomerId customerId,
                                 String customerName,
                                 String email)
        implements CustomerEvent {
}
