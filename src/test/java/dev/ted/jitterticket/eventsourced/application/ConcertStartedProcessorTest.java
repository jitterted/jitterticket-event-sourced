package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
    void concertScheduledEventsAddsShowDateTimeAlarmsForEach() {
        ConcertStartedProcessor concertStartedProcessor = new ConcertStartedProcessor();

        ConcertId firstConcertId = ConcertId.createRandom();
        LocalDateTime firstShowDateTime = oneWeekInTheFutureAtMidnight().plusHours(20);
        ConcertId secondConcertId = ConcertId.createRandom();
        LocalDateTime secondShowDateTime = oneMonthInTheFutureAtMidnight().plusHours(20);
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  firstConcertId,
                                  c -> c.showDateTime(firstShowDateTime))
                          .concertScheduled(
                                  secondConcertId,
                                  c -> c.showDateTime(secondShowDateTime))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .containsExactlyInAnyOrderEntriesOf(
                        Map.of(firstConcertId, firstShowDateTime,
                               secondConcertId, secondShowDateTime));

        // And AlarmScheduler was invoked with show date+time for ConcertId
    }

    static LocalDateTime oneWeekInTheFutureAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .plusWeeks(1);
    }

    static LocalDateTime oneWeekInThePastAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .minusWeeks(1);
    }

    static LocalDateTime oneMonthInTheFutureAtMidnight() {
        return LocalDateTime.now()
                            .truncatedTo(ChronoUnit.DAYS)
                            .plusMonths(1);
    }

    // handle schedule + reschedule

    @Test
    void concertRescheduledUpdatesAlarmToNewShowDateTime() {
        ConcertStartedProcessor concertStartedProcessor = new ConcertStartedProcessor();

        LocalDateTime showDateTime = oneWeekInTheFutureAtMidnight().plusHours(20);
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime rescheduledShowDateTime = showDateTime.plusWeeks(2);
        Stream<ConcertEvent> concertScheduledThenRescheduleStream =
                MakeEvents.with().concertScheduled(
                                  concertId,
                                  c -> c.showDateTime(showDateTime))
                          .reschedule(concertId,
                                      rescheduledShowDateTime,
                                      rescheduledShowDateTime.minusHours(1).toLocalTime())
                          .stream();

        concertStartedProcessor.handle(concertScheduledThenRescheduleStream);

        assertThat(concertStartedProcessor.alarms())
                .containsEntry(concertId, rescheduledShowDateTime);
    }

    // ignore events that are not Scheduled nor Rescheduled events


    @Test
    void ignoreNonSchedulingEvents() {
        ConcertStartedProcessor concertStartedProcessor = new ConcertStartedProcessor();
        ConcertId concertId = ConcertId.createRandom();
        Stream<ConcertEvent> concertEventStream =
                MakeEvents.with()
                          .concertScheduled(concertId,
                                            c -> c.ticketsSold(1))
                          .stream();

        concertStartedProcessor.handle(concertEventStream);

        assertThat(concertStartedProcessor.alarms())
                .extractingByKey(concertId)
                .isNotNull();
    }

    @Disabled("Until we handle events to be ignored")
    @Test
    void ignoreConcertsScheduledInThePast() {
        ConcertStartedProcessor concertStartedProcessor = new ConcertStartedProcessor();
        LocalDateTime showDateTime = oneWeekInThePastAtMidnight().plusHours(20);
        ConcertId concertId = ConcertId.createRandom();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  concertId,
                                  c -> c.showDateTime(showDateTime))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .as("Alarms should be empty as the only concert schedule event had a Show Date-Time in the past, so don't need to set an alarm.")
                .isEmpty();
    }

    // handle reschedule in the past: ignore

    // handle TicketSalesStopped by removing it from the alarms
}