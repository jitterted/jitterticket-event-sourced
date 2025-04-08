package dev.ted.jitterticket.eventsourced;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TixConfiguration {

    @Bean
    public EventStore<ConcertId, ConcertEvent, Concert> concertStore() {
        return EventStore.forConcerts();
    }

    @Bean
    public ConcertProjector concertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new ConcertProjector(concertStore);
    }

}
