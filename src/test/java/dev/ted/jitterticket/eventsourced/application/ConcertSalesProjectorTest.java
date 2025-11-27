package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertSalesProjectorTest {

    @Nested
    class NewProjector {

        private static final int MAX_CAPACITY = 100;
        private static final int MAX_TICKETS_PER_PURCHASE = 8;
        private static final List<ConcertSalesProjection> EMPTY_LOADED_PROJECTION_ROWS = Collections.emptyList();

        @Test
        void noScheduledConcertsReturnsNoSalesSummaries() {
            ConcertSalesProjector concertSalesProjector = new ConcertSalesProjector();

            List<ConcertSalesProjection> updatedProjectionRows =
                    concertSalesProjector.project(List.of(),
                                                  Stream.empty());

            assertThat(updatedProjectionRows)
                    .isEmpty();
        }

        @Test
        void singleConcertScheduledOnly_ProjectsSingleRow() {
            ConcertSalesProjector projector = new ConcertSalesProjector();
            ConcertId concertId = new ConcertId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
            LocalDateTime showDateTime = LocalDateTime.of(2026, 1, 1, 20, 0);
            Stream<ConcertEvent> concertEvents = Stream.of(
                    new ConcertScheduled(concertId,
                                         0,
                                         "The Beatles",
                                         42,
                                         showDateTime,
                                         LocalTime.now(),
                                         MAX_CAPACITY,
                                         MAX_TICKETS_PER_PURCHASE)
            );

            List<ConcertSalesProjection> updatedProjectionRows =
                    projector.project(EMPTY_LOADED_PROJECTION_ROWS, concertEvents);

            assertThat(updatedProjectionRows)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(new ConcertSalesProjection(
                            concertId.id(),
                            "The Beatles",
                            showDateTime.toLocalDate(),
                            0, 0));
        }

        @Test
        void singleConcertScheduled_SingleTicketPurchase_ProjectsSingleRowWithTicketSales() {
            ConcertSalesProjector projector = new ConcertSalesProjector();
            ConcertId concertId = new ConcertId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
            LocalDateTime showDateTime = LocalDateTime.of(2026, 1, 1, 20, 0);
            Stream<ConcertEvent> concertEvents = Stream.of(
                    new ConcertScheduled(concertId,
                                         0,
                                         "The Beatles",
                                         75,
                                         showDateTime,
                                         LocalTime.now(),
                                         MAX_CAPACITY,
                                         MAX_TICKETS_PER_PURCHASE),
                    new TicketsSold(concertId, 1, 4, 4 * 75)
            );

            List<ConcertSalesProjection> updatedProjectionRows =
                    projector.project(EMPTY_LOADED_PROJECTION_ROWS, concertEvents);

            assertThat(updatedProjectionRows)
                    .usingRecursiveFieldByFieldElementComparator()
                    .containsExactly(new ConcertSalesProjection(
                            concertId.id(),
                            "The Beatles",
                            showDateTime.toLocalDate(),
                            4,
                            4 * 75));

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

            ConcertSalesProjector concertSalesProjector = new ConcertSalesProjector();
            List<ConcertSalesProjection> concertSalesProjections =
                    concertSalesProjector.project(List.of(),
                                                  concertEventStream);

            assertThat(concertSalesProjections)
                    .extracting(ConcertSalesProjection::getConcertId,
                                ConcertSalesProjection::getTicketsSold,
                                ConcertSalesProjection::getTotalSales)
                    .containsExactlyInAnyOrder(
                            tuple(firstConcertId.id(), 4, 300),
                            tuple(secondConcertId.id(), 2 + 5, (2 + 5) * 50));
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
            List<ConcertSalesProjection> concertSalesProjections =
                    concertSalesProjector.project(List.of(),
                                                  concertEventStream);

            assertThat(concertSalesProjections)
                    // TODO: figure out how to extract and retain type safety here
                    .extracting(ConcertSalesProjection::getConcertId,
                                ConcertSalesProjection::getConcertDate)
                    .containsExactly(
                            tuple(concertId.id(), newShowDateTime.toLocalDate())
                    );
        }


        // TODO: test against the ConcertSalesSummary record "withers" directly,
        //       i.e., the plusTicketsSold and the reschedule

    }

    private static class MakeEvents {
        private int eventSequence = 0;
        private final List<ConcertEvent> events = new ArrayList<>();

        public static MakeEvents with() {
            return new MakeEvents();
        }


        public Stream<ConcertEvent> stream() {
            return events.stream();
        }

        public MakeEvents concertScheduled(ConcertId concertId, Function<ConcertCustomizer, ConcertCustomizer> concertCustomizer) {
            ConcertCustomizer customizer = concertCustomizer.apply(new ConcertCustomizer());
            ConcertScheduled concertScheduled = new ConcertScheduled(concertId,
                                                                     eventSequence++,
                                                                     "Don't Care Artist Name",
                                                                     customizer.ticketPrice,
                                                                     LocalDateTime.now(),
                                                                     LocalTime.now(),
                                                                     100,
                                                                     8);
            events.add(concertScheduled);
            customizer.ticketsSoldQuantity
                    .stream()
                    .map(qty -> new TicketsSold(
                            concertId,
                            eventSequence++,
                            qty,
                            qty * customizer.ticketPrice))
                    .forEach(events::add);
            return this;
        }

        public MakeEvents reschedule(ConcertId concertId, LocalDateTime newShowDateTime, LocalTime newDoorsTime) {
            events.add(
                    new ConcertRescheduled(concertId,
                                           eventSequence++,
                                           newShowDateTime,
                                           newDoorsTime));
            return this;
        }

        private static class ConcertCustomizer {

            private int ticketPrice;
            private final List<Integer> ticketsSoldQuantity = new ArrayList<>();

            public ConcertCustomizer ticketPrice(int ticketPrice) {
                this.ticketPrice = ticketPrice;
                return this;
            }

            public ConcertCustomizer ticketsSold(int quantity) {
                ticketsSoldQuantity.add(quantity);
                return this;
            }
        }
    }
}