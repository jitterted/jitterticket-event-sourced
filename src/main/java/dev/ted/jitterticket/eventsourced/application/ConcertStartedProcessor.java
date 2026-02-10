package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ConcertStartedProcessor implements EventConsumer<ConcertEvent> {

    private final Map<ConcertId, ConcertAlarm> alarmMap = new HashMap<>();
    private final ScheduledExecutorService scheduledExecutorService;
    private final Clock clock;

    private ConcertStartedProcessor(
            ScheduledExecutorService scheduledExecutorService,
            Clock clock) {
        this.scheduledExecutorService = scheduledExecutorService;
        this.clock = clock;
    }

    public static ConcertStartedProcessor create() {
        return new ConcertStartedProcessor(ForkJoinPool.commonPool(),
                                           Clock.systemDefaultZone());
    }

    public static ConcertStartedProcessor create(ScheduledExecutorService scheduledExecutorService) {
        return new ConcertStartedProcessor(scheduledExecutorService,
                                           Clock.systemDefaultZone());
    }

    public static ConcertStartedProcessor createForTest(
            ScheduledExecutorService scheduledExecutorService,
            Clock clock) {
        return new ConcertStartedProcessor(scheduledExecutorService, clock);
    }

    public Map<ConcertId, ConcertAlarm> alarms() {
        return Map.copyOf(alarmMap);
    }

    @Override
    public void handle(Stream<ConcertEvent> concertEventStream) {
        concertEventStream.forEach(
                concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled cs -> scheduleAlarm(cs.concertId(), cs.showDateTime());

                        case ConcertRescheduled cr ->
                                rescheduleAlarm(cr.concertId(), cr.newShowDateTime());

                        case TicketSalesStopped ticketSalesStopped ->
                                cancelAlarm(ticketSalesStopped.concertId());

                        case TicketsSold _ -> {
                            // ignore
                        }
                    }
                });
    }

    private void rescheduleAlarm(ConcertId concertId, LocalDateTime newShowDateTime) {
        if (inTheFuture(newShowDateTime)) {
            cancelAlarm(concertId);
            scheduleAlarm(concertId, newShowDateTime);
        }
    }

    private void cancelAlarm(ConcertId concertId) {
        ConcertAlarm removedAlarm = alarmMap.remove(concertId);
        if (removedAlarm != null) {
            removedAlarm.cancel();
        }
// Alternative (clever, but not as readable):
//        alarmMap.computeIfPresent(
//                concertId,
//                (_, concertAlarm) -> {
//                    concertAlarm.cancel();
//                    return null;
//                }
//        );
    }

    private void scheduleAlarm(ConcertId concertId,
                               LocalDateTime showDateTime) {
        if (inTheFuture(showDateTime)) {
            ScheduledFuture<?> scheduledFuture = scheduledExecutorService
                    .schedule(
                            () -> {},
                            delayFromNowInMinutes(showDateTime),
                            TimeUnit.MINUTES
                    );
            alarmMap.put(concertId, new ConcertAlarm(showDateTime, scheduledFuture));
        }
    }

    private boolean inTheFuture(LocalDateTime showDateTime) {
        return showDateTime.isAfter(LocalDateTime.now(clock));
    }

    private long delayFromNowInMinutes(LocalDateTime showDateTime) {
        return LocalDateTime.now(clock)
                            .until(showDateTime, ChronoUnit.MINUTES);
    }

}

record ConcertAlarm(LocalDateTime showDateTime,
                    ScheduledFuture<?> scheduledFuture) {
    void cancel() {
        scheduledFuture().cancel(false);
    }
}

