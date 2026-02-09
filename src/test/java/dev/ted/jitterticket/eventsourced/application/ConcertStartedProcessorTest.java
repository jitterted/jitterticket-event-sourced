package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertStartedProcessorTest {

    @Test
    void newProcessorHasNoAlarms() {
        ConcertStartedProcessor concertStartedProcessor = new ConcertStartedProcessor();

        assertThat(concertStartedProcessor.alarms())
                .as("Alarms should be empty for a newly created processor")
                .isEmpty();
    }

    @Test
    void concertScheduledAddsAlarmForShowDateTime() {
        ConcertStartedProcessor concertStartedProcessor = new ConcertStartedProcessor();

        LocalDateTime oneWeekInTheFutureAtMidnight = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusDays(7);
        LocalDateTime showDateTime = oneWeekInTheFutureAtMidnight.plusHours(20);
        ConcertId concertId = ConcertId.createRandom();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with().concertScheduled(
                                  concertId,
                                  c -> c.showDateTime(showDateTime))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .containsEntry(concertId, showDateTime);

        // And AlarmScheduler was invoked with show date+time for ConcertId
    }

    // handle schedule + reschedule

    // handle two separate schedules

    // handle schedule in the past: ignore

    // handle reschedule in the past: ignore
}