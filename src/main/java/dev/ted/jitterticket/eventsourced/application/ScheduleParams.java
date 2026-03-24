package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.in.web.LocalDateTimeFormatting;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public record ScheduleParams(String artist,
                             int ticketPrice,
                             LocalDateTime showDateTime,
                             LocalTime doorsTime,
                             int capacity,
                             int maxTicketsPerPurchase) {
    public ScheduleParams {
        LocalTime showTime = showDateTime.toLocalTime();
        Duration between = Duration.between(doorsTime, showTime);
        if (!doorsTime.isBefore(showTime)
            || between.compareTo(Duration.of(30, ChronoUnit.MINUTES)) < 0
            || between.compareTo(Duration.of(2, ChronoUnit.HOURS)) > 0) {
            throw new InvalidParamsException(
                    "Doors Time (%s) must be between 30 minutes and 2 hours before Show Time (%s)"
                            .formatted(showTime.format(LocalDateTimeFormatting.HH_MM_24_HOUR_FORMAT),
                                       doorsTime.format(LocalDateTimeFormatting.HH_MM_24_HOUR_FORMAT))
            );
        }
    }
}
