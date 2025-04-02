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
    void findByIdForNonExistingConcertReturnsEmptyOptional() {
        ConcertStore concertStore = new ConcertStore();

        ConcertId concertId = new ConcertId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        assertThat(concertStore.findById(concertId))
                .as("Should not be able to find a non-existent Concert by ID")
                .isEmpty();
    }

    @Test
    void findByIdReturnsSavedConcert() {
        ConcertStore concertStore = new ConcertStore();
        ConcertId concertId = new ConcertId(UUID.randomUUID());
        Concert concert = Concert.schedule(concertId,
                                           "Headliner",
                                           99,
                                           LocalDateTime.now(),
                                           LocalTime.now().minusHours(1),
                                           100,
                                           4
        );

        concertStore.save(concert);

        assertThat(concertStore.findById(concertId))
                .as("Should be able to find a saved Concert by its ConcertId")
                .isPresent()
                .get()
                .extracting(Concert::artist)
                .isEqualTo("Headliner");
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
        return Concert.schedule(new ConcertId(UUID.randomUUID()),
                                "Headliner",
                                99,
                                LocalDateTime.now(),
                                LocalTime.now().minusHours(1),
                                100,
                                4
        );
    }
}
