package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

        @Disabled("Obsolete: this test might fit in the Infra+Projector tests")
        @Test
        void singleConcertScheduledSingleTicketPurchaseReturnsCorrectSummaryOfSoldAndSales() {
            int ticketPrice = 75;
            Fixture fixture = scheduleAndSaveConcert(ticketPrice);
            Customer customer = CustomerFactory.reconstituteWithRegisteredEvent();
            int quantityPurchased = 4;
            fixture.concert.sellTicketsTo(customer.getId(), quantityPurchased);
            fixture.eventStore.save(fixture.concert);

            // TODO: compare:
            // (1) Granular:
            //      create stores
            //      create+save concert
            //      create+save customer
            //      concert.sellTicketsTo: customer, qty
            //      save concert
            // (2) Setup using PurchaseTicketsUseCase
            //      create stores
            //      create (schedule) Concert
            //      create (register) Customer
            //      create PTUC
            //      execute PTUC.purchaseTickets
            // (3) Use manually created events
            //      create stores
            //      store ConcertScheduled event
            //      store CustomerRegistered event (optional)
            //      store TicketsSold event (*required*)
            //      store TicketsPurchased event (optional)

            ConcertSalesProjector.ConcertSalesSummary expectedSummary =
                    new ConcertSalesProjector.ConcertSalesSummary(
                            fixture.concertId,
                            fixture.concert.artist(),
                            fixture.concert.showDateTime(),
                            4,
                            75 * 4);
            assertThat(fixture.concertSalesProjector.allSalesSummaries())
                    .singleElement().isEqualTo(expectedSummary);
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

        @Disabled("Convert to use I/O-free projector implementation")
        @Test
        void multipleConcertsWithTicketsSoldReturnsCorrectSummaryOfSales() {
            var concertEventStore = InMemoryEventStore.forConcerts();
            ConcertId concertId1 = ConcertId.createRandom();
            int ticketPrice1 = 35;
            LocalDateTime showDateTime1 = LocalDateTime.now();
            String artist1 = "First Concert";
            ConcertScheduled concertScheduled1 = new ConcertScheduled(concertId1, 0,
                                                                      artist1,
                                                                      ticketPrice1,
                                                                      showDateTime1,
                                                                      LocalTime.now(),
                                                                      MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            int quantity1 = 2;
            TicketsSold ticketsSoldForConcert1 = new TicketsSold(concertId1, 1, quantity1, quantity1 * ticketPrice1);
            concertEventStore.save(concertId1, Stream.of(concertScheduled1, ticketsSoldForConcert1));
            ConcertId concertId2 = ConcertId.createRandom();
            int ticketPrice2 = 125;
            LocalDateTime showDateTime2 = LocalDateTime.now();
            String artist2 = "Second Concert";
            ConcertScheduled concertScheduled2 = new ConcertScheduled(
                    concertId2, 0,
                    artist2,
                    ticketPrice2,
                    showDateTime2,
                    LocalTime.now(),
                    MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            int quantity2 = 8;
            TicketsSold ticketsSold1ForConcert2 = new TicketsSold(concertId2, 1, quantity2, quantity2 * ticketPrice2);
            int quantity3 = 6;
            TicketsSold ticketsSold2ForConcert2 = new TicketsSold(concertId2, 2, quantity3, quantity3 * ticketPrice2);
            concertEventStore.save(concertId2, Stream.of(concertScheduled2, ticketsSold1ForConcert2, ticketsSold2ForConcert2));
            ConcertSalesProjector concertSalesProjector =
                    ConcertSalesProjector.createForTest(concertEventStore);

            var expectedSummary1 = new ConcertSalesProjector.ConcertSalesSummary(
                    concertId1,
                    artist1,
                    showDateTime1,
                    quantity1,
                    ticketPrice1 * quantity1);
            var expectedSummary2 = new ConcertSalesProjector.ConcertSalesSummary(
                    concertId2,
                    artist2,
                    showDateTime2,
                    quantity2 + quantity3,
                    ticketsSold1ForConcert2.totalPaid()
                    + ticketsSold2ForConcert2.totalPaid());

            var actualMap = concertSalesProjector
                    .allSalesSummaries()
                    .collect(Collectors.toMap(ConcertSalesProjector.ConcertSalesSummary::artist, Function.identity()));
            var expectedMap = Map.of(expectedSummary1.artist(), expectedSummary1,
                                     expectedSummary2.artist(), expectedSummary2);
            assertThat(actualMap)
                    .usingRecursiveComparison()
                    .isEqualTo(expectedMap);

// List comparison highlights mismatched fields, but uses [0] and [1] to reference elements, which is potentially unhelpful with longer lists
//            List<ConcertSalesProjector.ConcertSalesSummary> actualList =
//                    concertSalesProjector.allSalesSummaries()
//                                         .sorted(Comparator.comparing(summary -> summary.concertId().id()))
//                                         .toList();
//            List<ConcertSalesProjector.ConcertSalesSummary> expectedList =
//                    Stream.of(expectedSummary2, expectedSummary1)
//                          .sorted(Comparator.comparing(summary -> summary.concertId().id()))
//                          .toList();
//
//            assertThat(actualList)
//                    .usingRecursiveComparison()
//                    .isEqualTo(expectedList);

// unhelpful assertion failure output
//            assertThat(concertSalesProjector.allSalesSummaries())
//                    .usingRecursiveFieldByFieldElementComparator()
//                    .containsExactly(expectedSummary1, expectedSummary2);
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

        @Disabled("Convert to use I/O-free projector implementation")
        @Test
        void summaryIsUpToDateAfterRescheduledConcert() {
            var concertEventStore = InMemoryEventStore.forConcerts();
            ConcertId concertId = ConcertId.createRandom();
            LocalDateTime originalShowDateTime = LocalDateTime.now();
            LocalTime originalDoorsTime = LocalTime.now();
            ConcertScheduled concertScheduled =
                    new ConcertScheduled(concertId, 0, "Artist",
                                         35,
                                         originalShowDateTime,
                                         originalDoorsTime,
                                         MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            LocalDateTime newShowDateTime = originalShowDateTime.plusMonths(1);
            ConcertRescheduled concertRescheduled =
                    new ConcertRescheduled(concertId, 1,
                                           newShowDateTime,
                                           originalDoorsTime.minusHours(1));
            concertEventStore.save(concertId, Stream.of(concertScheduled, concertRescheduled));
            ConcertSalesProjector concertSalesProjector =
                    ConcertSalesProjector.createForTest(concertEventStore);

            ConcertSalesProjector.ConcertSalesSummary expectedSummary =
                    new ConcertSalesProjector
                            .ConcertSalesSummary(concertId,
                                                 "Artist",
                                                 newShowDateTime,
                                                 0, 0);
            assertThat(concertSalesProjector.allSalesSummaries())
                    .as("Sales Summaries should have 1 summary, but did not.")
                    .singleElement()
                    .usingRecursiveComparison()
                    .isEqualTo(expectedSummary);
        }

        // TODO: test against the ConcertSalesSummary record "withers" directly,
        //       i.e., the plusTicketsSold and the reschedule

        private static Fixture scheduleAndSaveConcert(int ticketPrice) {
            var concertEventStore = InMemoryEventStore.forConcerts();
            ConcertId concertId = ConcertFactory.Store.saveScheduledConcert(
                    concertEventStore, ticketPrice);
            Concert concert = concertEventStore.findById(concertId).orElseThrow();
            ConcertSalesProjector concertSalesProjector =
                    ConcertSalesProjector.createForTest(concertEventStore);
            return new Fixture(concertId, concert, concertSalesProjector, concertEventStore);
        }

        private record Fixture(
                ConcertId concertId,
                Concert concert,
                ConcertSalesProjector concertSalesProjector,
                EventStore<ConcertId, ConcertEvent, Concert> eventStore
        ) {}
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