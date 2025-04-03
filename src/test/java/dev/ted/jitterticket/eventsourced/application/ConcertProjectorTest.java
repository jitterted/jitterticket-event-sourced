package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static dev.ted.jitterticket.eventsourced.domain.ConcertFactory.createConcert;
import static org.assertj.core.api.Assertions.*;

class ConcertProjectorTest {

    @Test
    void noConcertsCreatedProjectorReturnsNoConcerts() {
        ConcertProjector concertProjector = new ConcertProjector(EventStore.forConcerts());

        Stream<ConcertId> concertIds = concertProjector.allConcerts();

        assertThat(concertIds)
                .isEmpty();
    }

    @Test
    @Disabled("dev.ted.jitterticket.eventsourced.application.ConcertProjectorTest 4/3/25 13:43 — until EventStore provides a way to get alllll events")
    void projectorReturnsNewConcertSavedInConcertStore() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        Concert savedConcert = createConcert();
        concertStore.save(savedConcert);
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        Stream<ConcertId> concerts = concertProjector.allConcerts();

        assertThat(concerts)
                .containsExactly(savedConcert.getId());
    }
}