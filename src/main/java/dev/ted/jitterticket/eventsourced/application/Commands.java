package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

public class Commands {

    private final CommandExecutorFactory commandExecutorFactory;

    public Commands(CommandExecutorFactory commandExecutorFactory) {
        this.commandExecutorFactory = commandExecutorFactory;
    }

    public CommandWithParams<ConcertId, RescheduleParams> createRescheduleCommand() {
        CommandWithParams<ConcertId, RescheduleParams> command =
                commandExecutorFactory.wrapWithParams(
                        (concert, reschedule) ->
                                concert.rescheduleTo(
                                        reschedule.showDateTime(),
                                        reschedule.doorsTime()));
        return command;
    }

    public CreateWithParams<ConcertId, ScheduleParams> createScheduleCommand() {
        CreateWithParams<ConcertId, ScheduleParams> command =
                commandExecutorFactory.wrapForCreation(
                        scheduleParams -> Concert.schedule(
                                ConcertId.createRandom(),
                                scheduleParams.artist(),
                                scheduleParams.ticketPrice(),
                                scheduleParams.showDateTime(),
                                scheduleParams.doorsTime(),
                                scheduleParams.capacity(),
                                scheduleParams.maxTicketsPerPurchase()));
        return command;
    }

}
