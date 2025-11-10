package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ConcertSalesProjectorTest {

    @Nested
    class NewProjector {

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
        void singleConcertScheduledOneTicketTransactionReturnsCorrectSummaryOfSoldAndSales() {
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
            //      sellTicketsTo: concert + customer
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
            //      store CustomerRegistered event
            //      store TicketsSold event (*required*)
            //      store TicketsPurchased event

            ConcertSalesProjector.ConcertSalesSummary expectedSummary =
                    new ConcertSalesProjector
                            .ConcertSalesSummary(fixture.concertId,
                                                 fixture.concert.artist(),
                                                 fixture.concert.showDateTime(),
                                                 4,
                                                 75 * 4);
            assertThat(fixture.concertSalesProjector.allSalesSummaries())
                    .singleElement().isEqualTo(expectedSummary);
        }

        // TODO: test for multiple concerts with multiple tickets sold (purchased)

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