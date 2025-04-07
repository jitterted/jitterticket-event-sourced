package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;

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
        concertStore.save(ConcertFactory.createConcertWith(firstConcertId,
                                                           "First Concert",
                                                           99,
                                                           LocalDateTime.of(2025, 4, 20, 20, 0),
                                                           LocalTime.of(19, 0)));
        ConcertId secondConcertId = new ConcertId(UUID.randomUUID());
        concertStore.save(ConcertFactory.createConcertWith(secondConcertId,
                                                           "Second Concert",
                                                           111,
                                                           LocalDateTime.of(2025, 4, 21, 21, 0),
                                                           LocalTime.of(19, 30)));
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        Stream<ConcertTicketView> allConcertTicketViews = concertProjector.allConcertTicketViews();

        assertThat(allConcertTicketViews)
                .containsExactlyInAnyOrder(
                        new ConcertTicketView(firstConcertId,
                                              "First Concert",
                                              99,
                                              LocalDateTime.of(2025, 4, 20, 20, 0),
                                              LocalTime.of(19, 0))
                        , new ConcertTicketView(secondConcertId,
                                                "Second Concert",
                                                111,
                                                LocalDateTime.of(2025, 4, 21, 21, 0),
                                                LocalTime.of(19, 30))
                );
    }

    @Test
    void projectorReturnsSingleConcertForSavedAndRescheduledConcerts() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        ConcertProjector concertProjector = new ConcertProjector(concertStore);
        ConcertId concertId = new ConcertId(UUID.randomUUID());
        concertStore.save(ConcertFactory.createConcertWith(concertId,
                                                           "Desi Bells",
                                                           35,
                                                           LocalDateTime.of(2025, 4, 22, 19, 0),
                                                           LocalTime.of(18, 0)));
        Concert rescheduledConcert = concertStore.findById(concertId).orElseThrow();
        rescheduledConcert.rescheduleTo(LocalDateTime.of(2025, 7, 11, 20, 0),
                                        LocalTime.of(19, 0));
        concertStore.save(rescheduledConcert);

        Stream<ConcertTicketView> allConcertTicketViews =
                concertProjector.allConcertTicketViews();

        assertThat(allConcertTicketViews)
                .containsExactly(new ConcertTicketView(
                        concertId,
                        "Desi Bells",
                        35,
                        LocalDateTime.of(2025, 7, 11, 20, 0),
                        LocalTime.of(19, 0)));
    }
}