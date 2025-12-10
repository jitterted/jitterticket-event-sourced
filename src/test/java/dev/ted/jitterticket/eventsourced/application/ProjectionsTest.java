package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.EventStoreConfiguration;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjection;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.DataJdbcContainerTest;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.EventDboRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.JdbcEventStore;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadata;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ProjectionMetadataRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@Import(EventStoreConfiguration.class)
public class ProjectionsTest extends DataJdbcContainerTest {

    private static final int MAX_CAPACITY = 100;
    private static final int MAX_TICKETS_PER_PURCHASE = 8;
    private static final int TICKET_PRICE = 42;

    @Autowired
    EventDboRepository eventDboRepository;

    @Autowired
    ProjectionMetadataRepository projectionMetadataRepository;

    @Autowired
    ConcertSalesProjectionRepository concertSalesProjectionRepository;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;


    @Nested
    class ProjectionNewlyCreated {

        @Test
        void noEventsExistReturnsEmptyProjectionWithNewlyCreatedMetadata() {
            Projections projections = createProjectionUpdater();

            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    projections.allSalesSummaries();

            assertThat(allSalesSummaries)
                    .isEmpty();

            Optional<Long> lastGlobalEventSequenceSeenByProjectionName =
                    projectionMetadataRepository.lastGlobalEventSequenceSeenByProjectionName(ConcertSalesProjector.PROJECTION_NAME);
            assertThat(lastGlobalEventSequenceSeenByProjectionName)
                    .as("Expected the Metadata Repository to have an entry for the projection named: `%s`", ConcertSalesProjector.PROJECTION_NAME)
                    .isPresent()
                    .get()
                    .as("No events were processed by the projector, so its last seen global event sequence should be 0")
                    .isEqualTo(0L);
        }

        @Test
        void oneConcertScheduledAndNoTicketsPurchasedReturnsOneSalesSummaryWithZeroSales() {
            ConcertId concertId = ConcertId.createRandom();
            LocalDateTime showDateTime = LocalDateTime.of(2027, 2, 1, 20, 0);
            String artist = "First Concert";
            ConcertScheduled concertScheduled = new ConcertScheduled(concertId,
                                                                     0,
                                                                     artist,
                                                                     TICKET_PRICE,
                                                                     showDateTime,
                                                                     LocalTime.now(),
                                                                     MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            concertEventStore.save(concertId, Stream.of(concertScheduled));
            Projections projections = createProjectionUpdater();

            var allSalesSummaries = projections.allSalesSummaries();
            assertThat(allSalesSummaries)
                    .containsExactly(
                            new ConcertSalesProjector.ConcertSalesSummary(
                                    concertId, artist,
                                    LocalDate.of(2027, 2, 1).atStartOfDay(),
                                    0, 0
                            )
                    );
            assertThat(concertSalesProjectionRepository.count())
                    .isEqualTo(1);
            assertThat(projectionMetadataRepository.lastGlobalEventSequenceSeenByProjectionName(ConcertSalesProjector.PROJECTION_NAME))
                    .isPresent()
                    .get()
                    .isEqualTo(1L);
        }

        @Test
        void oneConcertScheduledOneTicketPurchasedReturnsOneSalesSummaryWithTicketSales() {
            ConcertId concertId = ConcertId.createRandom();
            Stream<ConcertEvent> concertEventStream =
                    MakeEvents.with()
                              .concertScheduled(concertId, (concert) -> concert
                                      .ticketPrice(35)
                                      .ticketsSold(6))
                              .stream();
            concertEventStore.save(concertId, concertEventStream);
            Projections projections = createProjectionUpdater();

            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    projections.allSalesSummaries();

            assertThat(allSalesSummaries)
                    .extracting(ConcertSalesProjector.ConcertSalesSummary::totalQuantity,
                                ConcertSalesProjector.ConcertSalesSummary::totalSales)
                    .containsExactly(
                            tuple(6, 6 * 35)
                    );
        }
    }

    @Nested
    class ProjectionDataPersisted {

        @Test
        void noNewEventsReturnsPersistedProjection() {
            ConcertSalesProjection concertSalesProjection = new ConcertSalesProjection(
                    ConcertId.createRandom().id(),
                    "Artist Name", LocalDate.now(), 0, 0);
            concertSalesProjectionRepository.save(concertSalesProjection);
            projectionMetadataRepository.save(
                    new ProjectionMetadata(
                            ConcertSalesProjector.PROJECTION_NAME, 1));

            Projections projections =
                    new Projections(new ConcertSalesProjector(),
                                    InMemoryEventStore.forConcerts(),
                                    projectionMetadataRepository,
                                    concertSalesProjectionRepository);
            var persistedSummaries = projections.allSalesSummaries();

            assertThat(persistedSummaries)
                    .containsExactly(concertSalesProjection.toSummary());
        }

        @Test
        void projectionUsesPreviouslyPersistedDataWhenUpdated() {
            // event store's max GES = 2
            // metadata: last GES = 1, 1 row in projection table
            // => should catch up 1 missing events after GES=1
            ConcertId concertId = ConcertId.createRandom();
            ConcertSalesProjection concertSalesProjection = new ConcertSalesProjection(
                    concertId.id(), "Artist Name", LocalDate.now(), 0, 0);
            concertSalesProjectionRepository.save(concertSalesProjection);
            projectionMetadataRepository.save(
                    new ProjectionMetadata(
                            ConcertSalesProjector.PROJECTION_NAME, 1));
            var concertEventStore = InMemoryEventStore.forConcerts();
            Stream<ConcertEvent> concertEventStream =
                    MakeEvents.with()
                              .concertScheduled(concertId, (concert) -> concert
                                      .ticketPrice(35)
                                      .ticketsSold(6))
                              .stream();
            concertEventStore.save(concertId, concertEventStream);
            Projections projections =
                    new Projections(new ConcertSalesProjector(),
                                    concertEventStore,
                                    projectionMetadataRepository,
                                    concertSalesProjectionRepository);

            var summaries = projections.allSalesSummaries();

            assertThat(summaries)
                    .extracting(ConcertSalesProjector.ConcertSalesSummary::totalQuantity)
                    .containsExactly(6);
        }
    }

    @Nested
    class ProjectorInitiatesSubscription {
        @Test
        void newSalesProjectorSubscribesWithLastGlobalEventSequenceOfZero() {
            ProjectionsTest.EventStoreSpy eventStoreSpy = new ProjectionsTest.EventStoreSpy();

            new Projections(new ConcertSalesProjector(),
                            eventStoreSpy,
                            projectionMetadataRepository,
                            concertSalesProjectionRepository
            );

            eventStoreSpy
                    .assertSubscribedAndEventsAfterInvoked(0L);
        }

        @Test
        void subscribeWith9WhenLastGlobalSequenceInProjectionTableHas9() {
            ProjectionsTest.EventStoreSpy eventStoreSpy = new ProjectionsTest.EventStoreSpy();
            ProjectionMetadata projectionMetadata =
                    new ProjectionMetadata(ConcertSalesProjector.PROJECTION_NAME,
                                           9L);
            projectionMetadataRepository.save(projectionMetadata);
            new Projections(new ConcertSalesProjector(),
                            eventStoreSpy,
                            projectionMetadataRepository,
                            concertSalesProjectionRepository
            );

            eventStoreSpy
                    .assertSubscribedAndEventsAfterInvoked(9L);
        }

    }

    @Nested
    class AllProjectedSummaries {

        @Test
        void loadsProjectionOfSingleConcertScheduledEventFromRepository() {
            ConcertId concertId = ConcertId.createRandom();
            String artist = "Artist";
            LocalDateTime showDateTime = LocalDate.now().atStartOfDay();
            concertSalesProjectionRepository.save(
                    new ConcertSalesProjection(
                            concertId.id(), artist,
                            showDateTime.toLocalDate(),
                            0, 0)
            );
            ProjectionMetadata projectionMetadata =
                    new ProjectionMetadata(ConcertSalesProjector.PROJECTION_NAME,
                                           1L);
            projectionMetadataRepository.save(projectionMetadata);
            Projections projections = new Projections(new ConcertSalesProjector(),
                                                      JdbcEventStore.forConcerts(eventDboRepository),
                                                      projectionMetadataRepository,
                                                      concertSalesProjectionRepository
            );


            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    projections.allSalesSummaries();

            assertThat(allSalesSummaries)
                    .containsExactly(
                            new ConcertSalesProjector.ConcertSalesSummary(
                                    concertId, artist,
                                    showDateTime,
                                    0, 0
                            )
                    );
        }
    }

    //region Fixture
    private Projections createProjectionUpdater() {
        return new Projections(new ConcertSalesProjector(),
                               concertEventStore,
                               projectionMetadataRepository,
                               concertSalesProjectionRepository);
    }
    //endregion


    //region Test Doubles
    @SuppressWarnings("rawtypes")
    static class EventStoreSpy implements EventStore {

        private boolean subscribeInvoked = false;
        private long subscribedLastGlobalEventSequence;
        private long allEventsAfterGlobalEventSequence;

        @Override
        public void save(EventSourcedAggregate aggregate) {
        }

        @Override
        public long save(Id aggregateId, Stream uncommittedEvents) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional findById(Id id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Stream allEvents() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List eventsForAggregate(Id id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void subscribe(ConcertSalesProjector concertSalesProjector, long lastGlobalEventSequence) {
            subscribeInvoked = true;
            subscribedLastGlobalEventSequence = lastGlobalEventSequence;
        }

        public void assertSubscribedAndEventsAfterInvoked(long expectedLastGlobalSequence) {
            assertThat(subscribeInvoked)
                    .as("Expected subscribe to be called")
                    .isTrue();

            assertThat(subscribedLastGlobalEventSequence)
                    .as("Expected subscribe to be called with the correct last global event sequence")
                    .isEqualTo(expectedLastGlobalSequence);

            assertThat(allEventsAfterGlobalEventSequence)
                    .as("Expected request for all events after the subscribed last global event sequence")
                    .isEqualTo(expectedLastGlobalSequence);
        }

        @Override
        public Stream allEventsAfter(long globalEventSequence) {
            allEventsAfterGlobalEventSequence = globalEventSequence;
            return Stream.empty();
        }
    }
    //endregion Test Doubles


    @Deprecated // need to pull out any remaining useful notes
    class OldStuff {

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

//            ConcertSalesProjector.ConcertSalesSummary expectedSummary =
//                    new ConcertSalesProjector.ConcertSalesSummary(
//                            fixture.concertId,
//                            fixture.concert.artist(),
//                            fixture.concert.showDateTime(),
//                            4,
//                            75 * 4);
//            assertThat(fixture.concertSalesProjector.allSalesSummaries())
//                    .singleElement().isEqualTo(expectedSummary);
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
                    new ConcertSalesProjector();

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

//            var actualMap = concertSalesProjector
//                    .allSalesSummaries()
//                    .collect(Collectors.toMap(ConcertSalesProjector.ConcertSalesSummary::artist, Function.identity()));
//            var expectedMap = Map.of(expectedSummary1.artist(), expectedSummary1,
//                                     expectedSummary2.artist(), expectedSummary2);
//            assertThat(actualMap)
//                    .usingRecursiveComparison()
//                    .isEqualTo(expectedMap);

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


        private static Fixture scheduleAndSaveConcert(int ticketPrice) {
            var concertEventStore = InMemoryEventStore.forConcerts();
            ConcertId concertId = ConcertFactory.Store.saveScheduledConcert(
                    concertEventStore, ticketPrice);
            Concert concert = concertEventStore.findById(concertId).orElseThrow();
            ConcertSalesProjector concertSalesProjector =
                    new ConcertSalesProjector();
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
