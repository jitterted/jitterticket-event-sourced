package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;

import static dev.ted.jitterticket.eventsourced.domain.ConcertFactory.createConcertWithId;
import static org.assertj.core.api.Assertions.*;

class ConcertProjectorTest {

    @Test
    void noConcertsCreatedProjectorReturnsNoConcerts() {
        ConcertProjector concertProjector = new ConcertProjector(EventStore.forConcerts());

        Stream<ConcertTicketView> concertTicketViews = concertProjector.allConcertTicketViews();

        assertThat(concertTicketViews)
                .isEmpty();
    }

    @Test
    void projectorReturnsConcertsSavedInConcertStore() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        ConcertId firstConcertId = new ConcertId(UUID.randomUUID());
        concertStore.save(createConcertWithId(firstConcertId));
        ConcertId secondConcertId = new ConcertId(UUID.randomUUID());
        concertStore.save(createConcertWithId(secondConcertId));
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        Stream<ConcertTicketView> allConcertTicketViews = concertProjector.allConcertTicketViews();

        assertThat(allConcertTicketViews)
                .extracting(ConcertTicketView::concertId)
                .containsExactlyInAnyOrder(firstConcertId, secondConcertId);
    }

    @Test
    void projectorReturnsSingleConcertForSavedAndRescheduledConcerts() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        ConcertProjector concertProjector = new ConcertProjector(concertStore);
        ConcertId concertId = new ConcertId(UUID.randomUUID());
        concertStore.save(createConcertWithId(concertId));
        Concert rescheduledConcert = concertStore.findById(concertId).orElseThrow();
        rescheduledConcert.rescheduleTo(LocalDateTime.now(), LocalTime.now().minusHours(1));
        concertStore.save(rescheduledConcert);

        Stream<ConcertId> allConcertIds = concertProjector.allConcerts();

        assertThat(allConcertIds)
                .containsExactly(concertId);
    }
}