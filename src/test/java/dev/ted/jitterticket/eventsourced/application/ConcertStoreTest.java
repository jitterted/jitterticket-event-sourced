package dev.ted.jitterticket.eventsourced.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConcertStoreTest {

    @Test
    void newConcertStoreHasNoConcerts() {
        ConcertStore concertStore = new ConcertStore();

        assertThat(concertStore.findAll())
                .as("There should be no concerts in a newly created ConcertStore")
                .isEmpty();
    }
}