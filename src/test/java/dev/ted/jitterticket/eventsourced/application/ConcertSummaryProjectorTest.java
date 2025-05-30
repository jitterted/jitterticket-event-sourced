package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertSummaryProjectorTest {

    @Test
    void noConcertsCreatedProjectorReturnsNoConcerts() {
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(InMemoryEventStore.forConcerts());

        Stream<ConcertSummary> concertTicketViews = concertSummaryProjector.allConcertSummaries();

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
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);

        Stream<ConcertSummary> allConcertTicketViews = concertSummaryProjector.allConcertSummaries();

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
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(ConcertFactory.scheduleConcertWith(concertId,
                                                             "Desi Bells",
                                                             35,
                                                             LocalDateTime.of(2025, 4, 22, 19, 0),
                                                             LocalTime.of(18, 0)));
        Concert rescheduledConcert = concertStore.findById(concertId).orElseThrow();
        rescheduledConcert.rescheduleTo(LocalDateTime.of(2025, 7, 11, 20, 0),
                                        LocalTime.of(19, 0));
        concertStore.save(rescheduledConcert);

        Stream<ConcertSummary> allConcertTicketViews =
                concertSummaryProjector.allConcertSummaries();

        assertThat(allConcertTicketViews)
                .containsExactly(new ConcertSummary(
                        concertId,
                        "Desi Bells",
                        35,
                        LocalDateTime.of(2025, 7, 11, 20, 0),
                        LocalTime.of(19, 0)));
    }

}