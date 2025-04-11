package dev.ted.jitterticket.eventsourced;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Configuration
public class TixConfiguration {

    @Bean
    public EventStore<ConcertId, ConcertEvent, Concert> concertStore() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        ConcertId concertId = new ConcertId(UUID.randomUUID());
        concertStore.save(Concert.schedule(
                concertId,
                "The Sonic Waves",
                45,
                LocalDateTime.of(2025, 7, 26, 20, 0),
                LocalTime.of(19, 0),
                100,
                4));
        // rest of data generated by my good friend Junie
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Midnight Rebels",
                55,
                LocalDateTime.of(2025, 9, 15, 21, 0),
                LocalTime.of(20, 0),
                150,
                4));

// Example 2: Jazz ensemble with limited capacity
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Blue Note Quartet",
                35,
                LocalDateTime.of(2025, 8, 22, 19, 30),
                LocalTime.of(18, 30),
                75,
                2));

// Example 3: Popular pop artist with higher capacity
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Stella Nova",
                65,
                LocalDateTime.of(2025, 10, 5, 20, 0),
                LocalTime.of(18, 30),
                250,
                6));

// Example 4: Indie folk band with afternoon show
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Woodland Echoes",
                30,
                LocalDateTime.of(2025, 7, 12, 16, 0),
                LocalTime.of(15, 0),
                120,
                4));

// Example 5: Electronic music DJ with late night show
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Pulse Wave",
                40,
                LocalDateTime.of(2025, 11, 8, 22, 30),
                LocalTime.of(21, 0),
                180,
                5));

// Example 6: Classical orchestra
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Metropolitan Symphony",
                70,
                LocalDateTime.of(2025, 12, 20, 19, 0),
                LocalTime.of(18, 0),
                200,
                3));

// Example 7: Alternative rock band
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Cosmic Drift",
                45,
                LocalDateTime.of(2026, 1, 17, 20, 0),
                LocalTime.of(19, 0),
                130,
                4));

// Example 8: Hip-hop artist
        concertStore.save(Concert.schedule(
                new ConcertId(UUID.randomUUID()),
                "Lyrical Storm",
                50,
                LocalDateTime.of(2025, 9, 30, 21, 0),
                LocalTime.of(19, 30),
                175,
                4));
        return concertStore;
    }

    @Bean
    public ConcertProjector concertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new ConcertProjector(concertStore);
    }

}
