package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import org.junit.jupiter.api.Disabled;
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
    @Disabled("dev.ted.jitterticket.eventsourced.application.ConcertStoreTest 4/1/25 13:14 — until EventSourcedAggregate does apply() during enqueue()")
    void findAllReturnsOnlySavedConcert() {
        ConcertStore concertStore = new ConcertStore();
        Concert concert = Concert.schedule("Headliner",
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
}
