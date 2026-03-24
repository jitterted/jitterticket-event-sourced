package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Commands {

    private final CommandExecutorFactory commandExecutorFactory;
    private final ProjectionCoordinator<ConcertEvent, ScheduledConcerts, ScheduledConcertsDelta> scheduledConcertsProjectionCoordinator;

    public Commands(CommandExecutorFactory commandExecutorFactory,
                    ProjectionCoordinator<ConcertEvent, ScheduledConcerts, ScheduledConcertsDelta> scheduledConcertsProjectionCoordinator) {
        this.commandExecutorFactory = commandExecutorFactory;
        this.scheduledConcertsProjectionCoordinator = scheduledConcertsProjectionCoordinator;
    }

    public CommandWithParams<ConcertId, RescheduleParams> createRescheduleCommand() {
        return commandExecutorFactory.wrapWithParams(
                (concert, reschedule) -> {
                    LocalDateTime showDateTime = reschedule.showDateTime();
                    ensureNoConflictFor(showDateTime);
                    // call "internal" (aggregate) command method
                    concert.rescheduleTo(
                            showDateTime,
                            reschedule.doorsTime());
                });
    }

    public CreateWithParams<ConcertId, ScheduleParams> createScheduleCommand() {
        return commandExecutorFactory.wrapForCreation(
                scheduleParams -> {
                    LocalDateTime showDateTime = scheduleParams.showDateTime();
                    // parameter validation would go here, e.g., showDateTime is in the future
                    //   and doorsTime is within N hours of the show's Time
                    LocalTime doorsTime = scheduleParams.doorsTime();
                    //   capacity is within some range
                    //   etc.
                    // external prerequisite/validation:
                    ensureNoConflictFor(showDateTime);
                    // call "internal" (aggregate) command method
                    return Concert.schedule(
                            ConcertId.createRandom(),
                            scheduleParams.artist(),
                            scheduleParams.ticketPrice(),
                            showDateTime,
                            doorsTime,
                            scheduleParams.capacity(),
                            scheduleParams.maxTicketsPerPurchase());
                });
    }

    private void ensureNoConflictFor(LocalDateTime showDateTime) {
        if (scheduledConcertsProjectionCoordinator
                .projection()
                .conflictsWith(showDateTime)) {
            throw new SchedulingConflictException("Scheduling Conflict: a concert is already scheduled for " + showDateTime.format(DateTimeFormatter.ISO_DATE));
        }
    }

}
