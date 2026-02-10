package dev.ted.jitterticket.eventsourced.application;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class LocalDateTimeFactory {
    public static LocalDateTime oneWeekInTheFutureAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .plusWeeks(1);
    }

    public static LocalDateTime oneWeekInThePastAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .minusWeeks(1);
    }

    public static LocalDateTime oneMonthInThePastAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .minusMonths(1);
    }

    public static LocalDateTime oneMonthInTheFutureAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .plusMonths(1);
    }
}
