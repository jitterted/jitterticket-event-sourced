package dev.ted.jitterticket.eventsourced.domain;

public record CustomerRegistered(String customerName, String email)
        implements CustomerEvent {
}
