package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

public class Commands {

    private final CommandExecutorFactory commandExecutorFactory;

    public Commands(CommandExecutorFactory commandExecutorFactory) {
        this.commandExecutorFactory = commandExecutorFactory;
    }

    public CommandWithParams<ConcertId, Reschedule> createRescheduleCommand() {
        CommandWithParams<ConcertId, Reschedule> command = commandExecutorFactory.wrapWithParams(
                (concert, reschedule) ->
                        concert.rescheduleTo(
                                reschedule.showDateTime(),
                                reschedule.doorsTime()));
        return command;
    }
}
