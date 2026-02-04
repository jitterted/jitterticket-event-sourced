package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class AvailableConcertsProjectorTest {

    @Test
    void projectReturnsEmptyNewStateWhenNoConcertsAreScheduled() {
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();

        DomainProjector.ProjectorResult<AvailableConcerts, AvailableConcertsDelta> projection =
                availableConcertsProjector.project(AvailableConcerts.EMPTY, Stream.empty());

        assertThat(projection.fullState().availableConcerts())
                .isEmpty();
        assertThat(projection.delta().isEmpty())
                .isTrue();
    }

    @Test
    void projectReturnsConcertSummaryWhenConcertIsScheduled() {
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();
        ConcertId concertId = ConcertId.createRandom();
        var concertScheduled = new ConcertScheduled(concertId,
                                                    1L,
                                                    "Concert Artist",
                                                    99,
                                                    LocalDateTime.of(2025, 4, 20, 20, 0),
                                                    LocalTime.of(19, 0),
                                                    100,
                                                    4);

        var projection = availableConcertsProjector.project(AvailableConcerts.EMPTY, Stream.of(concertScheduled));

        AvailableConcert expectedSummary = new AvailableConcert(concertId,
                                                                "Concert Artist",
                                                                99,
                                                                LocalDateTime.of(2025, 4, 20, 20, 0),
                                                                LocalTime.of(19, 0));
        assertThat(projection.fullState().availableConcerts())
                .containsExactly(expectedSummary);
        assertThat(projection.delta().upsertedConcerts())
                .containsExactly(expectedSummary);
    }

    @Test
    void projectReturnsRescheduledConcertSummaryWhenConcertIsRescheduled() {
        ConcertId concertId = ConcertId.createRandom();
        AvailableConcert initialSummary = new AvailableConcert(concertId,
                                                               "Artist",
                                                               35,
                                                               LocalDateTime.of(2025, 4, 22, 19, 0),
                                                               LocalTime.of(18, 0));
        AvailableConcerts initialState = new AvailableConcerts(List.of(initialSummary));
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();
        var concertRescheduled = new ConcertRescheduled(concertId,
                                                        2L,
                                                        LocalDateTime.of(2025, 7, 11, 20, 0),
                                                        LocalTime.of(19, 0));

        var projection = availableConcertsProjector.project(initialState, Stream.of(concertRescheduled));

        AvailableConcert expectedSummary = new AvailableConcert(concertId,
                                                                "Artist",
                                                                35,
                                                                LocalDateTime.of(2025, 7, 11, 20, 0),
                                                                LocalTime.of(19, 0));
        assertThat(projection.fullState().availableConcerts())
                .containsExactly(expectedSummary);
        assertThat(projection.delta().upsertedConcerts())
                .containsExactly(expectedSummary);
    }

    @Test
    void projectRemovesProjectedAvailableConcertWhenTicketSalesStopped() {
        ConcertId concertId = ConcertId.createRandom();
        AvailableConcert initialSummary = new AvailableConcert(concertId,
                                                               "Artist",
                                                               35,
                                                               LocalDateTime.of(2025, 4, 22, 19, 0),
                                                               LocalTime.of(18, 0));
        AvailableConcerts initialState = new AvailableConcerts(List.of(initialSummary));
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();

        var projectorResult = availableConcertsProjector.project(
                initialState,
                Stream.of(new TicketSalesStopped(concertId, 2L)));

        assertThat(projectorResult.fullState().availableConcerts())
                .as("Projection result should be empty after concert's ticket sales have stopped")
                .isEmpty();
        assertThat(projectorResult.delta().removedConcertIds())
                .as("Removed concert IDs should have the ID for the concert where ticket sales stopped")
                .containsExactly(concertId);
    }

    // test: during a catch-up, we may see a ConcertScheduled and a
    // TicketSalesStopped, which means the project would ignore it and
    // we'd never see it in a Delta, nor in the full state
    @Test
    void projectIgnoresConcertScheduledAndTicketSalesStoppedInOneStream() {

    }
}