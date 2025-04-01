package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

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
        Concert concert = Concert.schedule(99,
                                           LocalDateTime.now(),
                                           LocalTime.now().minusHours(1),
                                           100,
                                           4);

        concertStore.save(concert);

        assertThat(concertStore.findAll())
                .containsExactly(concert);
    }
}