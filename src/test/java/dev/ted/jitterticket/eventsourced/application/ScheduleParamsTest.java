package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.in.web.LocalDateTimeFormatting;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

class ScheduleParamsTest {

    @ParameterizedTest
    @CsvSource("""
               2026-04-02T20:00,18:00
               2026-04-02T20:00,19:00
               2026-04-02T20:00,19:30
               """)
    void noExceptionThrownIfDoorsTimeIsInRangeOfShowTime(
            LocalDateTime showDateTime, LocalTime doorsTime) {
        // Doors time needs to be at least 30 minutes prior
        // AND no more than 2 hours prior to Show time

        assertThat(new ScheduleParams("don't care", 42,
                                      showDateTime, doorsTime,
                                      42, 42))
                .isNotNull();
    }

    @ParameterizedTest
    @CsvSource("""
               2026-04-02T20:00,20:00
               2026-04-02T20:00,20:01
               2026-04-02T20:00,19:31
               2026-04-02T20:00,17:59
               """)
    void exceptionThrownIfDoorsTimeIsOutOfRangeOfShowTime(
            LocalDateTime showDateTime, LocalTime doorsTime) {
        // Doors time needs to be at least 30 minutes prior
        // AND no more than 2 hours prior to Show time

        assertThatThrownBy(() -> new ScheduleParams("don't care", 42,
                                                    showDateTime, doorsTime,
                                                    42, 42))
                .isExactlyInstanceOf(InvalidParamsException.class)
                .hasMessage("Doors Time (%s) must be between 30 minutes and 2 hours before Show Time (%s)"
                                    .formatted(showDateTime.toLocalTime().format(LocalDateTimeFormatting.HH_MM_24_HOUR_FORMAT),
                                               doorsTime.format(LocalDateTimeFormatting.HH_MM_24_HOUR_FORMAT)));
    }


}