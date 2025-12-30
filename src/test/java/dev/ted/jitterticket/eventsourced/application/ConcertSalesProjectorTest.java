package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionDbo;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertSalesProjectorTest {

    private static final int MAX_CAPACITY = 100;
    private static final int MAX_TICKETS_PER_PURCHASE = 8;
    private static final Stream<ConcertEvent> EMPTY_CONCERT_EVENT_STREAM = Stream.empty();

    @Test
    void noScheduledConcertsReturnsNoSalesSummaries() {
        ConcertSalesProjector projector = new ConcertSalesProjector();

        ConcertSalesProjector.ProjectionResult projectionResult =
                projector.project(new HashMap<>(),
                                  EMPTY_CONCERT_EVENT_STREAM);

        assertThat(projectionResult.lastEventSequenceSeen())
                .isZero();
        assertThat(projectionResult.salesSummaries())
                .as("Expected the SET of Concert Sales to be empty")
                .isEmpty();
    }

    @Test
    void singleConcertScheduledOnly_ProjectsSingleRow() {
        ConcertSalesProjector projector = new ConcertSalesProjector();
        ConcertId concertId = new ConcertId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        LocalDateTime showDateTime = LocalDateTime.of(2026, 1, 1, 20, 0);
        long eventSequence = 23L;
        Stream<ConcertEvent> concertEvents = Stream.of(
                new ConcertScheduled(concertId,
                                     eventSequence,
                                     "The Dessy Bells",
                                     42,
                                     showDateTime,
                                     LocalTime.now(),
                                     MAX_CAPACITY,
                                     MAX_TICKETS_PER_PURCHASE)
        );

        ConcertSalesProjector.ProjectionResult projectionResult =
                projector.project(new HashMap<>(), concertEvents);

        assertThat(projectionResult.lastEventSequenceSeen())
                .as("Last event sequence seen should be the event sequence of the single event processed")
                .isEqualTo(eventSequence);
        assertThat(projectionResult.salesSummaries())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new ConcertSalesProjector.ConcertSalesSummary(
                        concertId,
                        "The Dessy Bells",
                        showDateTime,
                        0, 0));
    }

    @Test
    void singleConcertScheduled_SingleTicketPurchase_ProjectsSingleRowWithTicketSales() {
        ConcertSalesProjector projector = new ConcertSalesProjector();
        ConcertId concertId = new ConcertId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        LocalDateTime showDateTime = LocalDateTime.of(2026, 1, 1, 20, 0);
        long firstEventSequence = 27L;
        long secondEventSequence = 29L;
        Stream<ConcertEvent> concertEvents = Stream.of(
                new ConcertScheduled(concertId,
                                     firstEventSequence,
                                     "The Dessy Bells",
                                     75,
                                     showDateTime,
                                     LocalTime.now(),
                                     MAX_CAPACITY,
                                     MAX_TICKETS_PER_PURCHASE),
                new TicketsSold(concertId, secondEventSequence, 4, 4 * 75)
        );

        ConcertSalesProjector.ProjectionResult projectionResult =
                projector.project(new HashMap<>(), concertEvents);

        assertThat(projectionResult.lastEventSequenceSeen())
                .as("Last event sequence seen should be the event sequence of the last event processed")
                .isEqualTo(secondEventSequence);
        assertThat(projectionResult.salesSummaries())
                .usingRecursiveFieldByFieldElementComparator()
                .containsExactly(new ConcertSalesProjector.ConcertSalesSummary(
                        concertId,
                        "The Dessy Bells",
                        showDateTime,
                        4,
                        4 * 75));
    }

    private static ConcertSalesProjectionDbo createEmptySnapshot() {
        return new ConcertSalesProjectionDbo("new_projection", 0L);
    }

    @Test
    void multipleConcerts_MultipleTicketsSoldPerConcert_ProjectsRowPerConcert() {
        ConcertId firstConcertId = ConcertId.createRandom();
        ConcertId secondConcertId = ConcertId.createRandom();
        Stream<ConcertEvent> concertEventStream =
                MakeEvents.with()
                          .concertScheduled(firstConcertId, (concert) -> concert
                                  .ticketPrice(75)
                                  .ticketsSold(4))
                          .concertScheduled(secondConcertId, (concert) -> concert
                                  .ticketPrice(50)
                                  .ticketsSold(2)
                                  .ticketsSold(5))
                          .stream();

        ConcertSalesProjector projector = new ConcertSalesProjector();
        ConcertSalesProjector.ProjectionResult projectionResult =
                projector.project(new HashMap<>(), concertEventStream);

        assertThat(projectionResult.salesSummaries())
                .extracting(ConcertSalesProjector.ConcertSalesSummary::concertId,
                            ConcertSalesProjector.ConcertSalesSummary::totalQuantity,
                            ConcertSalesProjector.ConcertSalesSummary::totalSales)
                .containsExactlyInAnyOrder(
                        tuple(firstConcertId, 4, 300),
                        tuple(secondConcertId, 2 + 5, (2 + 5) * 50));
    }

    @Test
    void singleConcertRescheduled_ProjectsUpdatedDate() {
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime newShowDateTime = LocalDateTime.of(2026, 1, 1, 19, 30);
        LocalTime newDoorsTime = LocalTime.of(18, 30);
        Stream<ConcertEvent> concertEventStream =
                MakeEvents.with()
                          .concertScheduled(concertId, Function.identity())
                          .reschedule(concertId, newShowDateTime, newDoorsTime)
                          .stream();

        ConcertSalesProjector concertSalesProjector = new ConcertSalesProjector();
        ConcertSalesProjectionDbo concertSalesProjection =
                concertSalesProjector.project(createEmptySnapshot(),
                                              concertEventStream);

        assertThat(concertSalesProjection.getConcertSales())
                // TODO: figure out how to extract and retain type safety here
                .extracting(ConcertSalesDbo::getConcertId,
                            ConcertSalesDbo::getConcertDate)
                .containsExactly(
                        tuple(concertId.id(), newShowDateTime.toLocalDate())
                );
    }

    @Test
    void updatesPreviouslyComputedProjectionWithNewEvents() {
        ConcertId concertId = ConcertId.createRandom();
        LocalDateTime showDateTime = LocalDateTime.of(2026, 12, 21, 20, 0);
        List<ConcertEvent> concertScheduledWithTicketsSoldEvents =
                MakeEvents.with()
                          .concertScheduled(concertId, (concert) -> concert
                                  .artistNamed("Artist Name")
                                  .showDate(showDateTime)
                                  .ticketPrice(35)
                                  .ticketsSold(6))
                          .list();
        ConcertEvent concertScheduledEvent = concertScheduledWithTicketsSoldEvents.getFirst();
        ConcertEvent ticketsSoldEvent = concertScheduledWithTicketsSoldEvents.getLast();
        ConcertSalesProjectionDbo expectedProjection = new ConcertSalesProjectionDbo(
                "new_projection",
                ticketsSoldEvent.eventSequence());
        expectedProjection.setConcertSales(Set.of(new ConcertSalesDbo(
                concertId.id(),
                "Artist Name",
                showDateTime.toLocalDate(),
                6, 6 * 35
        )));

        ConcertSalesProjector concertSalesProjector = new ConcertSalesProjector();
        ConcertSalesProjectionDbo initialProjection =
                concertSalesProjector.project(
                        createEmptySnapshot(),
                        Stream.of(concertScheduledEvent)
                );

        ConcertSalesProjectionDbo finalProjection =
                concertSalesProjector.project(initialProjection,
                                              Stream.of(ticketsSoldEvent)
                );

        assertThat(finalProjection)
                .usingRecursiveComparison()
                .isEqualTo(expectedProjection);
    }

    @Test
    void ticketsSoldWhenConcertScheduledEventNotSeenIsIgnoredThenProjectionIsEmpty() {
        ConcertId concertId = ConcertId.createRandom();
        TicketsSold ticketsSold = new TicketsSold(concertId, 1L, 3, 75);

        ConcertSalesProjector concertSalesProjector = new ConcertSalesProjector();
        ConcertSalesProjectionDbo concertSalesProjection =
                concertSalesProjector.project(createEmptySnapshot(),
                                              Stream.of(ticketsSold));

        assertThat(concertSalesProjection.getConcertSales())
                .isEmpty();
    }


    // TODO: test against the ConcertSalesSummary record "withers" directly,
    //       i.e., the plusTicketsSold and the reschedule

}

