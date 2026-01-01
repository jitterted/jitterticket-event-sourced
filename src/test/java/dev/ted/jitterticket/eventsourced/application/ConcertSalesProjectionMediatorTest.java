package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.EventStoreConfiguration;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionDbo;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.DataJdbcContainerTest;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.EventDboRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.JdbcEventStore;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@Import(EventStoreConfiguration.class)
public class ConcertSalesProjectionMediatorTest extends DataJdbcContainerTest {

    private static final int MAX_CAPACITY = 100;
    private static final int MAX_TICKETS_PER_PURCHASE = 8;
    private static final int TICKET_PRICE = 42;

    @Autowired
    EventDboRepository eventDboRepository;

    @Autowired
    ConcertSalesProjectionRepository concertSalesProjectionRepository;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertEventStore;

    @BeforeEach
    void beforeEach() {
        concertSalesProjectionRepository.deleteAll();
    }

    @Nested
    class ProjectionNewlyCreated {

        @Test
        void noEventsInEventStoreReturnsEmptyProjectionWithNewlyCreatedMetadata() {
            ConcertSalesProjectionMediator mediator = createProjectionMediator();

            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    mediator.allSalesSummaries();

            assertThat(allSalesSummaries)
                    .isEmpty();

            Optional<ConcertSalesProjectionDbo> concertSalesProjectionDbo = concertSalesProjectionRepository.findById(ConcertSalesProjectionMediator.PROJECTION_NAME);
            assertThat(concertSalesProjectionDbo)
                    .as("Expected the Repository to have an entry for the projection named: `%s`", ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .isPresent()
                    .get()
                    .extracting(ConcertSalesProjectionDbo::getLastEventSequenceSeen)
                    .as("No events were processed by the projector, so its last seen global event sequence should be 0")
                    .isEqualTo(0L);
        }

        @Test
        void oneConcertScheduledAndNoTicketsPurchasedReturnsOneSalesSummaryWithZeroSales() {
            ConcertId concertId = ConcertId.createRandom();
            LocalDateTime showDateTime = LocalDateTime.of(2027, 2, 1, 20, 0);
            String artist = "First Concert";
            ConcertEvent concertScheduled =
                    new ConcertScheduled(concertId,
                                         1L,
                                         artist,
                                         TICKET_PRICE,
                                         showDateTime,
                                         LocalTime.now(),
                                         MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            concertScheduled = concertEventStore.save(concertId, Stream.of(concertScheduled)).findFirst().orElseThrow();
            ConcertSalesProjectionMediator mediator = createProjectionMediator();

            ConcertSalesProjectionDbo concertSalesProjectionDbo = concertSalesProjectionRepository
                    .findById(ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .orElseThrow();
            assertThat(concertSalesProjectionDbo.getConcertSales())
                    .as("Expected the projection to have a single concert sale summary after processing a single event")
                    .hasSize(1);
            assertThat(concertSalesProjectionDbo.getLastEventSequenceSeen())
                    .as("After projecting from a single event, the 'last' event sequence for the ConcertSalesProjection should be the sequence for that event")
                    .isEqualTo(concertScheduled.eventSequence());
            var allSalesSummaries = mediator.allSalesSummaries();
            assertThat(allSalesSummaries)
                    .containsExactly(
                            new ConcertSalesProjector.ConcertSalesSummary(
                                    concertId, artist,
                                    LocalDate.of(2027, 2, 1).atStartOfDay(),
                                    0, 0
                            )
                    );
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
            ConcertSalesProjectionMediator mediator = createProjectionMediator();

            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    mediator.allSalesSummaries();

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
            ConcertSalesProjectionDbo concertSalesProjectionDbo = new ConcertSalesProjectionDbo(
                    ConcertSalesProjectionMediator.PROJECTION_NAME, 1);
            ConcertSalesDbo concertSalesDbo = new ConcertSalesDbo(
                    ConcertId.createRandom().id(),
                    "Artist Name", LocalDate.now(), 0, 0);
            concertSalesProjectionDbo.setConcertSales(Set.of(concertSalesDbo));
            concertSalesProjectionRepository.save(concertSalesProjectionDbo);

            ConcertSalesProjectionMediator mediator =
                    new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                                       InMemoryEventStore.forConcerts(),
                                                       concertSalesProjectionRepository);
            var persistedSummaries = mediator.allSalesSummaries();

            assertThat(persistedSummaries)
                    .containsExactly(ConcertSalesProjectionMediator.dboToSummary(concertSalesDbo));
        }

        @Test
        void projectionUsesPreviouslyPersistedDataWhenUpdated() {
            // event store's max event sequence = 2
            // metadata: last event sequence = 1, 1 concert row in projection table
            // => should catch up 1 missing events after event sequence=1
            ConcertId concertId = ConcertId.createRandom();
            ConcertSalesDbo concertSalesDbo = new ConcertSalesDbo(
                    concertId.id(), "Artist Name", LocalDate.now(), 0, 0);
            ConcertSalesProjectionDbo concertSalesProjectionDbo = new ConcertSalesProjectionDbo(
                    ConcertSalesProjectionMediator.PROJECTION_NAME, 1);
            concertSalesProjectionDbo.setConcertSales(Set.of(concertSalesDbo));
            var concertEventStore = InMemoryEventStore.forConcerts();
            Stream<ConcertEvent> concertEventStream =
                    MakeEvents.with()
                              .concertScheduled(concertId, (concert) -> concert
                                      .ticketPrice(35)
                                      .ticketsSold(6))
                              .stream();
            concertEventStore.save(concertId, concertEventStream);
            ConcertSalesProjectionMediator mediator =
                    new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                                       concertEventStore,
                                                       concertSalesProjectionRepository);

            var summaries = mediator.allSalesSummaries();

            assertThat(summaries)
                    .extracting(ConcertSalesProjector.ConcertSalesSummary::totalQuantity)
                    .containsExactly(6);
        }
    }

    @Nested
    class ProjectorCatchesUpAndInitiatesSubscription {
        @Test
        void newSalesProjectorSubscribesWithLastGlobalEventSequenceOfZero() {
            ConcertSalesProjectionMediatorTest.EventStoreSpy eventStoreSpy = new ConcertSalesProjectionMediatorTest.EventStoreSpy();

            new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                               eventStoreSpy,
                                               concertSalesProjectionRepository
            );

            eventStoreSpy
                    .assertSubscribedAndEventsAfterInvoked(0L);
        }

        @Test
        void subscribeWith9WhenLastGlobalSequenceInProjectionTableHas9() {
            ConcertSalesProjectionMediatorTest.EventStoreSpy eventStoreSpy = new ConcertSalesProjectionMediatorTest.EventStoreSpy();
            ConcertSalesProjectionDbo concertSalesProjectionDbo = new ConcertSalesProjectionDbo(
                    ConcertSalesProjectionMediator.PROJECTION_NAME, 9L);
            concertSalesProjectionRepository.save(concertSalesProjectionDbo);
            new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                               eventStoreSpy,
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
            ConcertSalesProjectionDbo concertSalesProjectionDbo = new ConcertSalesProjectionDbo(
                    ConcertSalesProjectionMediator.PROJECTION_NAME, 1L);
            concertSalesProjectionDbo.setConcertSales(Set.of(
                    new ConcertSalesDbo(
                            concertId.id(), artist,
                            showDateTime.toLocalDate(),
                            0, 0)
            ));
            concertSalesProjectionRepository.save(concertSalesProjectionDbo);
            ConcertSalesProjectionMediator mediator =
                    new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                                       JdbcEventStore.forConcerts(eventDboRepository),
                                                       concertSalesProjectionRepository
                    );


            Stream<ConcertSalesProjector.ConcertSalesSummary> allSalesSummaries =
                    mediator.allSalesSummaries();

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

    @Nested
    class HandlesNewEventsFromEventStoreSave {
        @Test
        void newProjectionUpdatesProjectionDataAndCheckpoint() {
            ConcertSalesProjectionMediator mediator = createProjectionMediator();
            ConcertId concertId = ConcertId.createRandom();
            Concert concert = ConcertFactory.createConcertWithId(concertId);

            concertEventStore.save(concert);

            ConcertSalesProjectionDbo concertSalesProjectionDbo = concertSalesProjectionRepository
                    .findById(ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .orElseThrow();
            assertThat(concertSalesProjectionDbo.getConcertSales())
                    .as("Expected Concert Sales Projection to have 1 entry for the 1 concert scheduled")
                    .hasSize(1);
            assertThat(concertSalesProjectionDbo.getLastEventSequenceSeen())
                    .as("Expected global event sequence checkpoint to be the sequence for the last (only) event handled and not 0")
                    .isNotZero();
        }

        @Test
        void existingProjectionUpdatesProjectionDataAndCheckpoint() {
            ConcertSalesProjectionMediator mediator = createProjectionMediator();
            List<ConcertEvent> firstTwoEvents =
                    MakeEvents.with()
                              .concertScheduled()
                              .concertScheduled()
                              .list();
            ConcertEvent firstEvent = firstTwoEvents.getFirst();
            firstEvent = concertEventStore.save(firstEvent.concertId(), Stream.of(firstEvent)).findFirst().orElseThrow();
            ConcertEvent secondEvent = firstTwoEvents.getLast();
            secondEvent = concertEventStore.save(secondEvent.concertId(), Stream.of(secondEvent)).findFirst().orElseThrow();
            mediator.handle(Stream.of(firstEvent, secondEvent));

            long eventSequenceBeforeSave = concertSalesProjectionRepository
                    .findLastEventSequenceSeenByProjectionName(ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .orElseThrow();

            concertEventStore.save(ConcertFactory.createConcert());

            ConcertSalesProjectionDbo concertSalesProjectionDbo = concertSalesProjectionRepository
                    .findById(ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .orElseThrow();
            assertThat(concertSalesProjectionDbo.getConcertSales())
                    .as("Expected Concert Sales Projection to have 3 entries")
                    .hasSize(3);
            assertThat(concertSalesProjectionDbo.getLastEventSequenceSeen())
                    .as("Expected event sequence checkpoint to be the last event sequence that was handled")
                    .isGreaterThan(eventSequenceBeforeSave);
        }

        @Test
        void existingProjectionUnchangedWhenEmptyEventStreamHandled() {
            ConcertSalesProjectionMediator mediator = createProjectionMediator();
            concertEventStore.save(ConcertFactory.createConcert());
            ConcertSalesProjectionDbo initialProjectionDbo = concertSalesProjectionRepository
                    .findById(ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .orElseThrow();

            mediator.handle(Stream.empty());

            ConcertSalesProjectionDbo afterProjectionDbo = concertSalesProjectionRepository
                    .findById(ConcertSalesProjectionMediator.PROJECTION_NAME)
                    .orElseThrow();

            assertThat(afterProjectionDbo)
                    .as("Expected Concert Sales Projection to be unchanged after handling an empty stream of events")
                    .usingRecursiveComparison()
                    .isEqualTo(initialProjectionDbo);
        }

    }

    //region Fixture
    private ConcertSalesProjectionMediator createProjectionMediator() {
        return new ConcertSalesProjectionMediator(new ConcertSalesProjector(),
                                                  concertEventStore,
                                                  concertSalesProjectionRepository);
    }
    //endregion


    //region Test Doubles
    @SuppressWarnings("rawtypes")
    static class EventStoreSpy implements EventStore {

        private boolean subscribeInvoked = false;
        private long allEventsAfterGlobalEventSequence;

        @Override
        public void save(EventSourcedAggregate aggregate) {
        }

        @Override
        public Stream save(Id aggregateId, Stream uncommittedEvents) {
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
        public void subscribe(ConcertSalesProjectionMediator eventConsumer) {
            subscribeInvoked = true;
        }

        public void assertSubscribedAndEventsAfterInvoked(long expectedLastGlobalSequence) {
            assertThat(subscribeInvoked)
                    .as("Expected subscribe to be called")
                    .isTrue();

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
            ConcertScheduled concertScheduled1 = new ConcertScheduled(concertId1,
                                                                      1L,
                                                                      artist1,
                                                                      ticketPrice1,
                                                                      showDateTime1,
                                                                      LocalTime.now(),
                                                                      MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            int quantity1 = 2;
            TicketsSold ticketsSoldForConcert1 = new TicketsSold(concertId1, 2L, quantity1, quantity1 * ticketPrice1);
            concertEventStore.save(concertId1, Stream.of(concertScheduled1, ticketsSoldForConcert1));
            ConcertId concertId2 = ConcertId.createRandom();
            int ticketPrice2 = 125;
            LocalDateTime showDateTime2 = LocalDateTime.now();
            String artist2 = "Second Concert";
            ConcertScheduled concertScheduled2 = new ConcertScheduled(
                    concertId2, 1L,
                    artist2,
                    ticketPrice2,
                    showDateTime2,
                    LocalTime.now(),
                    MAX_CAPACITY, MAX_TICKETS_PER_PURCHASE);
            int quantity2 = 8;
            TicketsSold ticketsSold1ForConcert2 = new TicketsSold(concertId2, 1L, quantity2, quantity2 * ticketPrice2);
            int quantity3 = 6;
            TicketsSold ticketsSold2ForConcert2 = new TicketsSold(concertId2, 2L, quantity3, quantity3 * ticketPrice2);
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
