package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertSummaryProjectorTest {

    @Test
    void noConcertsCreatedProjectorReturnsNoConcerts() {
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(EventStore.forConcerts());

        Stream<ConcertSummary> concertTicketViews = concertSummaryProjector.allConcertSummaries();

        assertThat(concertTicketViews)
                .isEmpty();
    }

    @Test
    void projectorReturnsConcertsSavedInConcertStore() {
        var concertStore = EventStore.forConcerts();
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
        var concertStore = EventStore.forConcerts();
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

    @Test
    void projectsStateOnlyThroughDesiredEvent() {
        var concertStore = EventStore.forConcerts();
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 4, 22, 19, 0);
        LocalTime originalDoorsTime = LocalTime.of(18, 0);
        ConcertScheduled concertScheduled = new ConcertScheduled(concertId, 0, "Headliner", 45, originalShowDateTime, originalDoorsTime, 150, 8);
        TicketsSold ticketsSold = new TicketsSold(concertId, 1, 4, 4 * 45);
        ConcertRescheduled concertRescheduled = new ConcertRescheduled(concertId, 2, originalShowDateTime.plusMonths(2).plusHours(1), originalDoorsTime.plusHours(1));
        concertStore.save(concertId, Stream.of(concertScheduled, ticketsSold, concertRescheduled));

        int desiredEventSequenceNumber = ticketsSold.eventSequence();
        var concertWithEvents = concertSummaryProjector.concertWithEventsThrough(concertId, desiredEventSequenceNumber);

        assertThat(concertWithEvents.concertEvents())
                .containsExactly(concertScheduled, ticketsSold);
        assertThat(concertWithEvents.concert().showDateTime())
                .as("Show Date/Time should be the original as we shouldn't have played back the rescheduling event")
                .isEqualTo(originalShowDateTime);
    }
}