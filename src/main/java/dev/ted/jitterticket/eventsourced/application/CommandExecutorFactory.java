package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

public class CommandExecutorFactory {
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;
    private final ConcertQuery concertQuery;

    private CommandExecutorFactory(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.concertEventStore = concertEventStore;
        concertQuery = new ConcertQuery(concertEventStore);
    }

    public static CommandExecutorFactory create(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        return new CommandExecutorFactory(concertEventStore);
    }

    public static CommandExecutorFactory createForTest() {
        return create(InMemoryEventStore.forConcerts());
    }

    public Command<ConcertId> wrap(Command<Concert> concertCommand) {
        return concertId -> {
            Concert concert = concertQuery.find(concertId);
            concertCommand.execute(concert);
            concertEventStore.save(concert);
        };
    }

    public CommandWithParams<ConcertId, Reschedule> wrapWithParams(
            CommandWithParams<Concert, Reschedule> command) {
        return (concertId, reschedule) -> {
            Concert concert = concertQuery.find(concertId);
            command.execute(concert, reschedule);
            concertEventStore.save(concert);
        };
    }
}

