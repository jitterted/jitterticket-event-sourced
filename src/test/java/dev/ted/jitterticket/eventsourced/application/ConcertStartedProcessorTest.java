package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.*;

class ConcertStartedProcessorTest {

    @Test
    void newProcessorHasNoAlarms() {
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest();

        assertThat(concertStartedProcessor.alarms())
                .as("Alarms should be empty for a newly created processor")
                .isEmpty();
    }

    @Test
    void scheduledAlarmWrapsCommandInRunnableForExecutorService() {
        SpyScheduledExecutorService spyScheduledExecutorService =
                new SpyScheduledExecutorService();
        var concertEventStore = InMemoryEventStore.forConcerts();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(
                        spyScheduledExecutorService,
                        concertEventStore);
        ConcertId concertId = ConcertId.createRandom();
        List<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  concertId,
                                  c -> c.showDateTime(
                                          LocalDateTimeFactory.withNow()
                                                              .oneWeekInTheFutureAtMidnight()))
                          .list();
        concertEventStore.save(concertId, concertScheduledStream.stream());
        concertStartedProcessor.handle(concertScheduledStream.stream());

        Runnable command = spyScheduledExecutorService.scheduledCommands()
                                                      .getFirst()
                                                      .command();
        command.run();

        assertThat(concertEventStore.eventsForAggregate(concertId))
                .hasExactlyElementsOfTypes(ConcertScheduled.class,
                                           TicketSalesStopped.class);
    }

    @Test
    void concertScheduledEventsAddsShowDateTimeAlarmsForEach() {
        LocalDateTimeFactory localDateTimeFactory = LocalDateTimeFactory.withFixedClockAtMidnightUtc();
        SpyScheduledExecutorService spyScheduledExecutorService =
                new SpyScheduledExecutorService();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(
                        spyScheduledExecutorService,
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

        Map<ConcertId, ConcertAlarm> alarms = concertStartedProcessor.alarms();
        assertThat(alarms.values())
                .extracting(ConcertAlarm::showDateTime)
                .containsExactlyInAnyOrder(firstShowDateTime, secondShowDateTime);

        assertThat(alarms.values())
                .extracting(ConcertAlarm::scheduledFuture)
                .allMatch(not(Future::isCancelled));

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
        SpyScheduledExecutorService spyScheduledExecutorService = new SpyScheduledExecutorService();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(spyScheduledExecutorService);

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

        ConcertAlarm concertAlarm = concertStartedProcessor.alarms()
                                                           .get(concertId);
        assertThat(concertAlarm.showDateTime())
                .isEqualTo(rescheduledShowDateTime);
        assertThat(concertAlarm.scheduledFuture().isCancelled())
                .as("Rescheduled alarm's future should not be cancelled")
                .isFalse();

        assertThat(spyScheduledExecutorService.scheduledCommands())
                .hasSize(2);
        assertThat(spyScheduledExecutorService.scheduledCommands()
                                              .getFirst()
                                              .scheduledFuture()
                                              .isCancelled())
                .as("First scheduled command should be cancelled because of the reschedule")
                .isTrue();
        assertThat(spyScheduledExecutorService.scheduledCommands()
                                              .get(1) // 2nd command
                                              .scheduledFuture()
                                              .isCancelled())
                .as("Second scheduled command should NOT be cancelled because it's the new scheduled date-time")
                .isFalse();
    }

    @Test
    void ignoreTicketsSoldEvents() {
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.createForTest();
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
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.createForTest();
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
        ConcertStartedProcessor concertStartedProcessor = ConcertStartedProcessor.createForTest();
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
    void concertScheduledInThePastRescheduledInTheFutureResultsInSingleActiveAlarm() {
        SpyScheduledExecutorService spyScheduledExecutorService = new SpyScheduledExecutorService();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(spyScheduledExecutorService);
        LocalDateTimeFactory now = LocalDateTimeFactory.withNow();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  ConcertId.createRandom(),
                                  c -> c.showDateTime(now.oneMonthInThePastAtMidnight())
                                        .rescheduleTo(now.oneWeekInTheFutureAtMidnight()))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .values()
                .singleElement()
                .extracting(ConcertAlarm::scheduledFuture)
                .matches(not(Future::isCancelled));
        assertThat(spyScheduledExecutorService.scheduledCommands())
                .hasSize(1);
    }

    @Test
    void concertScheduledInFutureRescheduledToPastMustCancelAlarm() {
        SpyScheduledExecutorService spyScheduledExecutorService = new SpyScheduledExecutorService();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(spyScheduledExecutorService);
        LocalDateTimeFactory now = LocalDateTimeFactory.withNow();
        Stream<ConcertEvent> concertScheduledStream =
                MakeEvents.with()
                          .concertScheduled(
                                  ConcertId.createRandom(),
                                  c -> c.showDateTime(now.oneMonthInTheFutureAtMidnight())
                                        .rescheduleTo(now.oneWeekInThePastAtMidnight()))
                          .stream();

        concertStartedProcessor.handle(concertScheduledStream);

        assertThat(concertStartedProcessor.alarms())
                .as("Rescheduling to the past should cancel (remove) alarm originally scheduled")
                .isEmpty();
    }

    @Test
    void alarmCanceledWhenTicketSalesStopped() {
        SpyScheduledExecutorService spyScheduledExecutorService = new SpyScheduledExecutorService();
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.createForTest(spyScheduledExecutorService);
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
        assertThat(spyScheduledExecutorService.scheduledCommands())
                .singleElement()
                .extracting(ScheduledCommand::scheduledFuture)
                .matches(Future::isCancelled, "Since ticket sales for the concert were stopped, the scheduled future must be canceled");
    }
}

class SpyScheduledExecutorService extends ForkJoinPool {
    private final List<ScheduledCommand> scheduledCommands = new ArrayList<>();

    public List<ScheduledCommand> scheduledCommands() {
        return scheduledCommands;
    }

    @Override
    public @NotNull ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        ScheduledFuture<?> scheduledFuture = super.schedule(command, delay, unit);
        scheduledCommands.add(new ScheduledCommand(command, delay, unit, scheduledFuture));
        return scheduledFuture;
    }
}

record ScheduledCommand(Runnable command, long delay, TimeUnit unit,
                        ScheduledFuture<?> scheduledFuture) {}
