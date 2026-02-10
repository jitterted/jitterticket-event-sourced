package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertStartedProcessorTest {

    @Test
    void newProcessorHasNoAlarms() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.create();

        assertThat(concertStartedProcessor.alarms())
                .as("Alarms should be empty for a newly created processor")
                .isEmpty();
    }
    
    @Test
    void concertScheduledEventsAddsShowDateTimeAlarmsForEach() {
        LocalDateTimeFactory localDateTimeFactory = LocalDateTimeFactory.withFixedClockAtMidnightUtc();
        SpyScheduledExecutorService spyScheduledExecutorService =
                new SpyScheduledExecutorService();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(spyScheduledExecutorService,
                                                      localDateTimeFactory.clock());

        ConcertId firstConcertId = ConcertId.createRandom();
        LocalDateTime firstShowDateTime = localDateTimeFactory.oneWeekInTheFutureAtMidnight().plusHours(20);
        ConcertId secondConcertId = ConcertId.createRandom();
        LocalDateTime secondShowDateTime = localDateTimeFactory.oneMonthInTheFutureAtMidnight().plusHours(20);
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

        long firstConcertExpectedDelay = 7 * 24 * 60 + (20 * 60); // 1 week + 20 hours
        // can't calculate this next one without know how long "1 month" is, so will leave it as-is
        long secondConcertExpectedDelay = localDateTimeFactory.now().until(secondShowDateTime, ChronoUnit.MINUTES);
        assertThat(spyScheduledExecutorService.scheduledCommands())
                .extracting(ScheduledCommand::delay, ScheduledCommand::unit)
                .containsExactly(tuple(firstConcertExpectedDelay, TimeUnit.MINUTES),
                                 tuple(secondConcertExpectedDelay, TimeUnit.MINUTES));
    }

    @Test
    void concertRescheduledUpdatesAlarmToNewShowDateTime() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.create();

        LocalDateTime showDateTime = LocalDateTimeFactory.withNow().oneWeekInTheFutureAtMidnight().plusHours(20);
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime rescheduledShowDateTime = showDateTime.plusWeeks(2);
        Stream<ConcertEvent> concertScheduledThenRescheduleStream =
                MakeEvents.with().concertScheduled(
                                  concertId,
                                  c -> c.showDateTime(showDateTime)
                                        .rescheduleTo(rescheduledShowDateTime))
                          .stream();

        concertStartedProcessor.handle(concertScheduledThenRescheduleStream);

        assertThat(concertStartedProcessor.alarms())
                .containsEntry(concertId, rescheduledShowDateTime);
    }

    @Test
    void ignoreTicketsSoldEvents() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.create();
        ConcertId concertId = ConcertId.createRandom();
        Stream<ConcertEvent> concertEventStream =
                MakeEvents.with()
                          .concertScheduled(
                                  concertId,
                                  c -> c.showDateTime(LocalDateTimeFactory.withNow().oneWeekInTheFutureAtMidnight())
                                        .ticketsSold(1))
                          .stream();

        concertStartedProcessor.handle(concertEventStream);

        assertThat(concertStartedProcessor.alarms())
                .extractingByKey(concertId)
                .isNotNull();
    }

    @Test
    void ignoreConcertsScheduledInThePast() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.create();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  ConcertId.createRandom(),
                                  c -> c.showDateTime(LocalDateTimeFactory.withNow().oneWeekInThePastAtMidnight()))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .as("Alarms should be empty as the only concert schedule event had a Show Date-Time in the past, so don't need to set an alarm.")
                .isEmpty();
    }

    @Test
    void ignoreConcertsRescheduledInThePast() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.create();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  ConcertId.createRandom(),
                                  c -> c.showDateTime(LocalDateTimeFactory.withNow().oneMonthInThePastAtMidnight())
                                        .rescheduleTo(LocalDateTimeFactory.withNow().oneWeekInThePastAtMidnight()))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .as("Alarms should be empty as the only concert schedule event had a Show Date-Time in the past, so don't need to set an alarm.")
                .isEmpty();

    }

    @Test
    void alarmCanceledWhenTicketSalesStopped() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.create();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  ConcertId.createRandom(),
                                  c -> c.showDateTime(LocalDateTimeFactory.withNow().oneMonthInTheFutureAtMidnight())
                                        .ticketSalesStopped())
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .as("TicketSalesStopped should have removed (canceled) the alarm set by the ConcertScheduled event.")
                .isEmpty();
    }
}

class SpyScheduledExecutorService extends ForkJoinPool {
    private final List<ScheduledCommand> scheduledCommands = new ArrayList<>();

    public List<ScheduledCommand> scheduledCommands() {
        return scheduledCommands;
    }

    @Override
    public @NotNull ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        scheduledCommands.add(new ScheduledCommand(command, delay, unit));
        return super.schedule(command, delay, unit);
    }
}

record ScheduledCommand(Runnable command, long delay, TimeUnit unit) {}
