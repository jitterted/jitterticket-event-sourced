package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.List;

public class CommandExecutorFactory {
    private final EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    private CommandExecutorFactory(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        this.concertEventStore = concertEventStore;
    }

    public static CommandExecutorFactory create(EventStore<ConcertId, ConcertEvent, Concert> concertEventStore) {
        return new CommandExecutorFactory(concertEventStore);
    }

    public Command<ConcertId> wrap(Command<Concert> concertCommand) {
        return concertId -> {
            List<ConcertEvent> concertEvents = concertEventStore
                    .eventsForAggregate(concertId);
            Concert concert = Concert.reconstitute(concertEvents);
            concertCommand.execute(concert);
            concertEventStore.save(concert);
        };
    }
}
