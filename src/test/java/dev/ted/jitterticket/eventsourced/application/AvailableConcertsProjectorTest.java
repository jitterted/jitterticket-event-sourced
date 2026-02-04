package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
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

        var projection = availableConcertsProjector.project(
                AvailableConcerts.EMPTY, Stream.empty());

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
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();
        Stream<ConcertEvent> eventStream = MakeEvents
                .withNullEventSequences()
                .concertScheduled(ConcertId.createRandom(),
                                  MakeEvents.ConcertCustomizer::ticketSalesStopped)
                .stream();

        var projection = availableConcertsProjector.project(
                AvailableConcerts.EMPTY, eventStream);

        assertThat(projection.fullState().availableConcerts())
                .as("Full projection should be empty as scheduling and then stopping ticket sales is a no-op")
                .isEmpty();
        assertThat(projection.delta().upsertedConcerts())
                .as("Upserted concerts should be empty as scheduling and then stopping ticket sales in a single projector execution is a no-op")
                .isEmpty();
        assertThat(projection.delta().removedConcertIds())
                .as("Removed concert IDs should be empty as scheduling and then stopping ticket sales in a single projector execution is a no-op")
                .isEmpty();
    }

    @Test
    void projectShouldStillRemoveConcertIdAfterConcertRescheduledAndTicketSalesStopped() {
        ConcertId concertId = ConcertId.createRandom();
        AvailableConcert initialSummary = new AvailableConcert(
                concertId,
                "Artist",
                35,
                LocalDateTime.of(2025, 4, 22, 19, 0),
                LocalTime.of(18, 0));
        AvailableConcerts initialState = new AvailableConcerts(List.of(initialSummary));
        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();

        ConcertRescheduled concertRescheduled = new ConcertRescheduled(
                concertId,
                2L,
                LocalDateTime.of(2025, 7, 11, 20, 0),
                LocalTime.of(19, 0));
        TicketSalesStopped ticketSalesStopped = new TicketSalesStopped(
                concertId, 3L);

        var projectorResult = availableConcertsProjector.project(
                initialState,
                Stream.of(concertRescheduled, ticketSalesStopped));

        assertThat(projectorResult.fullState().availableConcerts())
                .as("Full projection should be empty as rescheduling and then stopping ticket sales should mean no concerts available")
                .isEmpty();
        assertThat(projectorResult.delta().upsertedConcerts())
                .as("Upserted concerts should be empty as rescheduling and then stopping ticket sales in a single projector execution means there's no reason to update a concert we're going to remove")
                .isEmpty();
        assertThat(projectorResult.delta().removedConcertIds())
                .as("Removed concert IDs should have the concert ID to remove as stopping the ticket sales is a removal from the projection, regardless of it being rescheduled")
                .containsExactly(concertId);
    }
}