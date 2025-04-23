package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertProjectorTest {

    @Test
    void noConcertsCreatedProjectorReturnsNoConcerts() {
        ConcertProjector concertProjector = new ConcertProjector(EventStore.forConcerts());

        Stream<ConcertSummary> concertTicketViews = concertProjector.allConcertSummaries();

        assertThat(concertTicketViews)
                .isEmpty();
    }

    @Test
    void projectorReturnsConcertsSavedInConcertStore() {
        var concertStore = EventStore.forConcerts();
        ConcertId firstConcertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.createConcertWith(firstConcertId,
                                                           "First Concert",
                                                           99,
                                                           LocalDateTime.of(2025, 4, 20, 20, 0),
                                                           LocalTime.of(19, 0)));
        ConcertId secondConcertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.createConcertWith(secondConcertId,
                                                           "Second Concert",
                                                           111,
                                                           LocalDateTime.of(2025, 4, 21, 21, 0),
                                                           LocalTime.of(19, 30)));
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        Stream<ConcertSummary> allConcertTicketViews = concertProjector.allConcertSummaries();

        assertThat(allConcertTicketViews)
                .containsExactlyInAnyOrder(
                        new ConcertSummary(firstConcertId,
                                           "First Concert",
                                           99,
                                           LocalDateTime.of(2025, 4, 20, 20, 0),
                                           LocalTime.of(19, 0))
                        , new ConcertSummary(secondConcertId,
                                             "Second Concert",
                                             111,
                                             LocalDateTime.of(2025, 4, 21, 21, 0),
                                             LocalTime.of(19, 30))
                );
    }

    @Test
    void projectorReturnsSingleConcertForSavedAndRescheduledConcerts() {
        var concertStore = EventStore.forConcerts();
        ConcertProjector concertProjector = new ConcertProjector(concertStore);
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.createConcertWith(concertId,
                                                           "Desi Bells",
                                                           35,
                                                           LocalDateTime.of(2025, 4, 22, 19, 0),
                                                           LocalTime.of(18, 0)));
        Concert rescheduledConcert = concertStore.findById(concertId).orElseThrow();
        rescheduledConcert.rescheduleTo(LocalDateTime.of(2025, 7, 11, 20, 0),
                                        LocalTime.of(19, 0));
        concertStore.save(rescheduledConcert);

        Stream<ConcertSummary> allConcertTicketViews =
                concertProjector.allConcertSummaries();

        assertThat(allConcertTicketViews)
                .containsExactly(new ConcertSummary(
                        concertId,
                        "Desi Bells",
                        35,
                        LocalDateTime.of(2025, 7, 11, 20, 0),
                        LocalTime.of(19, 0)));
    }
}