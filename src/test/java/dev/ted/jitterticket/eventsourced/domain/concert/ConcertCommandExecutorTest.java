package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.application.Command;
import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ConcertCommandExecutorTest {

    @Test
    void stopTicketSalesCommandStoresNewEventInEventStore() {
        var concertEventStore = InMemoryEventStore.forConcerts();
        ConcertId concertId = ConcertId.createRandom();
        concertEventStore.save(ConcertFactory.createConcertWithId(concertId));
        CommandExecutorFactory factory = CommandExecutorFactory.create(concertEventStore);

        Command<Concert> command = Concert::stopTicketSales;
        Command<ConcertId> commandExecutor = factory.wrap(command);

        commandExecutor.execute(concertId);

        List<ConcertEvent> events = concertEventStore.eventsForAggregate(concertId);
        assertThat(events)
                .hasExactlyElementsOfTypes(ConcertScheduled.class,
                                           TicketSalesStopped.class);
        assertThat(events.get(1).concertId())
                .isEqualTo(concertId);


    }
}