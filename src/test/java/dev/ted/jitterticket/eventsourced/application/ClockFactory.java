package dev.ted.jitterticket.eventsourced.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class ClockFactory {
    public static Clock fixedClockAt(int year, int month, int dayOfMonth) {
        LocalDateTime now = LocalDateTime.of(year, month, dayOfMonth, 0, 0);
        return Clock.fixed(
                now.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
    }
}
