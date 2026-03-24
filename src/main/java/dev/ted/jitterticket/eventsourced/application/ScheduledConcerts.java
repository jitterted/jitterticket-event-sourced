package dev.ted.jitterticket.eventsourced.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record ScheduledConcerts(List<ScheduledConcert> scheduledConcerts) {
    static final ScheduledConcerts EMPTY = new ScheduledConcerts(List.of());

    public boolean conflictsWith(LocalDateTime localDateTime) {
        LocalDate targetLocalDate = localDateTime.toLocalDate();
        return scheduledConcerts
                .stream()
                .anyMatch(scheduledConcert ->
                                  scheduledConcert
                                          .showDate()
                                          .isEqual(targetLocalDate));
    }
}
