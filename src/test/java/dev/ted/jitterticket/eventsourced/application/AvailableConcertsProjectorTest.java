package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class AvailableConcertsProjectorTest {

    @Test
    void noConcertsCreatedProjectorReturnsNoConcerts() {
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector(InMemoryEventStore.forConcerts());

        Stream<ConcertSummary> concertTicketViews = availableConcertsProjector.availableConcerts();

        assertThat(concertTicketViews)
                .isEmpty();
    }

    @Test
    void projectorReturnsConcertsSavedInConcertStore() {
        var concertStore = InMemoryEventStore.forConcerts();
        ConcertId firstConcertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(firstConcertId,
                                                             "First Concert",
                                                             99,
                                                             LocalDateTime.of(2025, 4, 20, 20, 0),
                                                             LocalTime.of(19, 0)));
        ConcertId secondConcertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(secondConcertId,
                                                             "Second Concert",
                                                             111,
                                                             LocalDateTime.of(2025, 4, 21, 21, 0),
                                                             LocalTime.of(19, 30)));
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector(concertStore);

        Stream<ConcertSummary> allConcertTicketViews = availableConcertsProjector.availableConcerts();

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
        var concertStore = InMemoryEventStore.forConcerts();
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(concertId,
                                                             "Desi Bells",
                                                             35,
                                                             LocalDateTime.of(2025, 4, 22, 19, 0),
                                                             LocalTime.of(18, 0)));
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector(concertStore);
        Concert rescheduledConcert = concertStore.findById(concertId).orElseThrow();
        rescheduledConcert.rescheduleTo(LocalDateTime.of(2025, 7, 11, 20, 0),
                                        LocalTime.of(19, 0));
        concertStore.save(rescheduledConcert);

        Stream<ConcertSummary> allConcertTicketViews =
                availableConcertsProjector.availableConcerts();

        assertThat(allConcertTicketViews)
                .containsExactly(new ConcertSummary(
                        concertId,
                        "Desi Bells",
                        35,
                        LocalDateTime.of(2025, 7, 11, 20, 0),
                        LocalTime.of(19, 0)));
    }

}