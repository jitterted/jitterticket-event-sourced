package dev.ted.jitterticket.eventsourced.domain;

import java.util.UUID;

public record TicketOrderId(UUID id) {
    public static TicketOrderId createRandom() {
        return new TicketOrderId(UUID.randomUUID());
    }
}
