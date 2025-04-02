package dev.ted.jitterticket.eventsourced.domain;

public sealed interface CustomerEvent extends Event
        permits CustomerRegistered {
}
