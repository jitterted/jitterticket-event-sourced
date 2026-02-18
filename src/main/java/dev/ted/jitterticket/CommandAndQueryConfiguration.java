package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.application.ConcertQuery;
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

    // reschedule and other command objects

    //endregion Command Objects

}
