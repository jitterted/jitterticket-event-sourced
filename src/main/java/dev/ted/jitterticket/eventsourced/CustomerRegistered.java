package dev.ted.jitterticket.eventsourced;

public record CustomerRegistered(String customerName, String email) implements CustomerEvent {
}
