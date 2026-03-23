package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ScheduledConcertsProjectorTest {

    @Test
    void projectReturnsEmptyNewStateWhenNoConcertsAreScheduled() {
        ScheduledConcertsProjector scheduledConcertsProjector = new ScheduledConcertsProjector();

        var projection = scheduledConcertsProjector.project(
                ScheduledConcerts.EMPTY, Stream.empty());

        assertThat(projection.fullState().scheduledConcerts())
                .isEmpty();
        assertThat(projection.delta().isEmpty())
                .isTrue();
    }

    @Test
    void projectReturnsConcertSummaryWhenConcertIsScheduled() {
        ScheduledConcertsProjector scheduledConcertsProjector = new ScheduledConcertsProjector();
        ConcertId concertId = ConcertId.createRandom();
        var concertScheduled = new ConcertScheduled(concertId,
                                                    1L,
                                                    "Don't Care",
                                                    42,
                                                    LocalDateTime.of(2025, 4, 20, 20, 0),
                                                    LocalTime.of(19, 0),
                                                    42,
                                                    42);

        var projection = scheduledConcertsProjector
                .project(ScheduledConcerts.EMPTY, Stream.of(concertScheduled));

        ScheduledConcert expectedSummary = new ScheduledConcert(
                concertId, LocalDate.of(2025, 4, 20));
        assertThat(projection.fullState().scheduledConcerts())
                .as("The full state of the projection should have the scheduled concert")
                .containsExactly(expectedSummary);
        assertThat(projection.delta().upsertedConcerts())
                .as("The scheduled concert should be in the list of 'upserted' concerts")
                .containsExactly(expectedSummary);
    }

//    @Test
//    void projectReturnsRescheduledConcertSummaryWhenConcertIsRescheduled() {
//        ConcertId concertId = ConcertId.createRandom();
//        AvailableConcert initialSummary = new AvailableConcert(concertId,
//                                                               "Artist",
//                                                               35,
//                                                               LocalDateTime.of(2025, 4, 22, 19, 0),
//                                                               LocalTime.of(18, 0));
//        AvailableConcerts initialState = new AvailableConcerts(List.of(initialSummary));
//        AvailableConcertsProjector availableConcertsProjector = new AvailableConcertsProjector();
//        var concertRescheduled = new ConcertRescheduled(concertId,
//                                                        2L,
//                                                        LocalDateTime.of(2025, 7, 11, 20, 0),
//                                                        LocalTime.of(19, 0));
//
//        var projection = availableConcertsProjector.project(initialState, Stream.of(concertRescheduled));
//
//        AvailableConcert expectedSummary = new AvailableConcert(concertId,
//                                                                "Artist",
//                                                                35,
//                                                                LocalDateTime.of(2025, 7, 11, 20, 0),
//                                                                LocalTime.of(19, 0));
//        assertThat(projection.fullState().availableConcerts())
//                .containsExactly(expectedSummary);
//        assertThat(projection.delta().upsertedConcerts())
//                .containsExactly(expectedSummary);
//    }

}