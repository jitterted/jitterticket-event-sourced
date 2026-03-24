package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.CommandWithParams;
import dev.ted.jitterticket.eventsourced.application.Commands;
import dev.ted.jitterticket.eventsourced.application.ConcertQuery;
import dev.ted.jitterticket.eventsourced.application.CreateWithParams;
import dev.ted.jitterticket.eventsourced.application.ProjectionCoordinator;
import dev.ted.jitterticket.eventsourced.application.RescheduleParams;
import dev.ted.jitterticket.eventsourced.application.ScheduleParams;
import dev.ted.jitterticket.eventsourced.application.ScheduledConcerts;
import dev.ted.jitterticket.eventsourced.application.ScheduledConcertsDelta;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandAndQueryConfiguration {

    //region Query Objects
    @Bean
    ConcertQuery concertQuery(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new ConcertQuery(concertStore);
    }
    //endregion Query Objects

    //region Command Objects
    @Bean
    public CommandExecutorFactory commandExecutorFactory(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return CommandExecutorFactory.create(concertStore);
    }

    @Bean
    Commands commands(CommandExecutorFactory commandExecutorFactory,
                      ProjectionCoordinator<ConcertEvent, ScheduledConcerts, ScheduledConcertsDelta> projectionCoordinator) {
        return new Commands(commandExecutorFactory, projectionCoordinator);
    }

    @Bean
    CommandWithParams<ConcertId, RescheduleParams> rescheduleCommand(Commands commands) {
        return commands.createRescheduleCommand();
    }

    @Bean
    CreateWithParams<ConcertId, ScheduleParams> scheduleCommand(Commands commands) {
        return commands.createScheduleCommand();
    }

    //endregion Command Objects

}
