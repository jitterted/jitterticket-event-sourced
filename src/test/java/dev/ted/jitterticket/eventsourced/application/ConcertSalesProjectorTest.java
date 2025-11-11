package dev.ted.jitterticket.eventsourced.application;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class ConcertSalesProjectorTest {

    @Nested
    class NewProjector {

        private static final int MAX_CAPACITY = 100;
        private static final int MAX_TICKETS_PER_PURCHASE = 8;

        @Test
        void noScheduledConcertsReturnsNoSalesSummaries() {
            ConcertSalesProjector concertSalesProjector = ConcertSalesProjector.createForTest();

            assertThat(concertSalesProjector.allSalesSummaries())
                    .isEmpty();
        }

        @Test
        void onlyScheduledConcertAndNoTicketSalesReturnsSalesSummaryWithZeroes() {
            Fixture fixture = scheduleAndSaveConcert(42);

            ConcertSalesProjector.ConcertSalesSummary expectedSummary =
                    new ConcertSalesProjector
                            .ConcertSalesSummary(fixture.concertId,
                                                 fixture.concert.artist(),
                                                 fixture.concert.showDateTime(),
                                                 0, 0);
            assertThat(fixture.concertSalesProjector().allSalesSummaries())
                    .containsExactly(expectedSummary);
        }

        @Test
        void singleConcertScheduledSingleTicketPurchaseReturnsCorrectSummaryOfSoldAndSales() {
            int ticketPrice = 75;
            Fixture fixture = scheduleAndSaveConcert(ticketPrice);
            Customer customer = CustomerFactory.reconstituteWithRegisteredEvent();
            int quantityPurchased = 4;
            fixture.concert.sellTicketsTo(customer.getId(), quantityPurchased);
            fixture.concertSalesProjector.apply(fixture.concert.uncommittedEvents());
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

}