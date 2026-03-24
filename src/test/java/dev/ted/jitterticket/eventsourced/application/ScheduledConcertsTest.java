package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ScheduledConcertsTest {

    @Test
    void noConflictWhenScheduledConcertsIsEmpty() {
        ScheduledConcerts scheduledConcerts = ScheduledConcerts.EMPTY;

        boolean hasConflict = scheduledConcerts
                .conflictsWith(LocalDateTime.of(2026, 4, 1, 19, 0));

        assertThat(hasConflict)
                .as("Expected no conflict when there are no scheduled concerts")
                .isFalse();
    }

    @Test
    void noConflictWhenScheduledConcertsOnOtherDates() {
        ScheduledConcerts scheduledConcerts = new ScheduledConcerts(List.of(
                new ScheduledConcert(ConcertId.createRandom(),
                                     LocalDate.of(2026, 4, 1)),
                new ScheduledConcert(ConcertId.createRandom(),
                                     LocalDate.of(2026, 4, 2))));

        boolean hasConflict = scheduledConcerts
                .conflictsWith(LocalDateTime.of(2026, 4, 3, 21, 0));

        assertThat(hasConflict)
                .as("Expected no conflict when scheduled concerts are on other dates")
                .isFalse();
    }

    @Test
    void conflictWhenScheduledConcertExistsForGivenDateTime() {
        ScheduledConcerts scheduledConcerts = new ScheduledConcerts(List.of(
                new ScheduledConcert(ConcertId.createRandom(),
                                     LocalDate.of(2026, 4, 2))));

        boolean hasConflict = scheduledConcerts
                .conflictsWith(LocalDateTime.of(2026, 4, 2, 20, 0));

        assertThat(hasConflict)
                .as("Expected conflict when scheduled concert date already exists")
                .isTrue();
    }
}