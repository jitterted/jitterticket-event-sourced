package dev.ted.jitterticket.eventsourced.application;

import java.util.List;

public record ScheduledConcerts(List<ScheduledConcert> scheduledConcerts) {
    static final ScheduledConcerts EMPTY = new ScheduledConcerts(List.of());
}
