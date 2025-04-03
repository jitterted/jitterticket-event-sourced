package dev.ted.jitterticket.eventsourced.domain;

public record CustomerRegistered(CustomerId customerId, String customerName, String email)
        implements CustomerEvent {
}
