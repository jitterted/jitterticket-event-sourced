package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ConcertStoreTest {

    @Test
    void newConcertStoreHasNoConcerts() {
        ConcertStore concertStore = new ConcertStore();

        assertThat(concertStore.findAll())
                .as("There should be no concerts in a newly created ConcertStore")
                .isEmpty();
    }

    @Test
    void findAllReturnsOnlySavedConcert() {
        ConcertStore concertStore = new ConcertStore();
        Concert concert = Concert.schedule(new ConcertId(UUID.randomUUID()), "Headliner",
                                           99,
                                           LocalDateTime.now(),
                                           LocalTime.now().minusHours(1),
                                           100,
                                           4
        );

        concertStore.save(concert);

        assertThat(concertStore.findAll())
                .hasSize(1)
                .extracting(Concert::artist)
                .containsExactly("Headliner");
    }

    @Test
    void findReturnsDifferentInstanceOfConcert() {
        ConcertStore concertStore = new ConcertStore();
        Concert savedConcert = createConcert();
        concertStore.save(savedConcert);

        Concert foundConcert = concertStore.findAll().findFirst().orElseThrow();

        assertThat(foundConcert)
                .isNotSameAs(savedConcert);
    }

    private Concert createConcert() {
        return Concert.schedule(new ConcertId(UUID.randomUUID()), "Headliner",
                                99,
                                LocalDateTime.now(),
                                LocalTime.now().minusHours(1),
                                100,
                                4
        );
    }
}
