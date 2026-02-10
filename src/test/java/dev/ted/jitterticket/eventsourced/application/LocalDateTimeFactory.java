package dev.ted.jitterticket.eventsourced.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class LocalDateTimeFactory {

    private final Clock clock;
    private final LocalDateTime localDateTime;

    private LocalDateTimeFactory(Clock clock) {
        this.localDateTime = LocalDateTime.now(clock);
        this.clock = clock;
    }

    public static LocalDateTimeFactory withNow() {
        return new LocalDateTimeFactory(Clock.systemDefaultZone());
    }

    public static LocalDateTimeFactory with(Clock clock) {
        return new LocalDateTimeFactory(clock);
    }

    public static LocalDateTimeFactory withFixedClockAtMidnightUtc() {
        Clock fixedClockAtMidnightUtc = Clock.fixed(
                Instant.now().truncatedTo(ChronoUnit.DAYS), ZoneId.of("UTC"));
        return with(fixedClockAtMidnightUtc);
    }

    public LocalDateTime oneWeekInTheFutureAtMidnight() {
        return localDateTime
                .truncatedTo(ChronoUnit.DAYS)
                .plusWeeks(1);
    }

    public LocalDateTime oneWeekInThePastAtMidnight() {
        return localDateTime
                .truncatedTo(ChronoUnit.DAYS)
                .minusWeeks(1);
    }

    public LocalDateTime oneMonthInThePastAtMidnight() {
        return localDateTime
                .truncatedTo(ChronoUnit.DAYS)
                .minusMonths(1);
    }

    public LocalDateTime oneMonthInTheFutureAtMidnight() {
        return localDateTime
                .truncatedTo(ChronoUnit.DAYS)
                .plusMonths(1);
    }

    public LocalDateTime now() {
        return localDateTime;
    }

    public Clock clock() {
        return clock;
    }
}
