package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.ArrayListStringsReaderAppender;
import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvReaderAppender;
import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvStringsEventStore;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

public class EventStoreTest {

    @TempDir
    static Path tempDir;

    static Stream<Arguments> concertEventStoreSupplier() throws IOException {
        Path tempFile = Files.createTempFile(tempDir, "test", ".csv");
        return Stream.of(
                Arguments.of(Named.of("In-Memory", InMemoryEventStore.forConcerts()))
                ,
                Arguments.of(Named.of("CSV ArrayList", CsvStringsEventStore.forConcerts(new ArrayListStringsReaderAppender())))
                ,
                Arguments.of(Named.of("CSV File", CsvStringsEventStore.forConcerts(new CsvReaderAppender(tempFile))))
        );
    }

    @Nested
    public class ConcertEventStoreFindById {

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void findByIdForNonExistingConcertReturnsEmptyOptional(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId nonExistentConcertId = ConcertId.createRandom();
            assertThat(concertStore.findById(nonExistentConcertId))
                    .as("Should not be able to find a non-existent Concert by ID")
                    .isEmpty();
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void findByIdReturnsSavedConcert(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId = ConcertId.createRandom();
            Concert concert = Concert.schedule(concertId,
                                               "Headliner",
                                               99,
                                               LocalDateTime.now(),
                                               LocalTime.now().minusHours(1),
                                               100,
                                               4
            );

            concertStore.save(concert);

            assertThat(concertStore.findById(concertId))
                    .as("Should be able to find a saved Concert by its ConcertId")
                    .isPresent()
                    .get()
                    .extracting(Concert::artist)
                    .isEqualTo("Headliner");
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void findByIdReturnsDifferentInstanceOfConcert(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert savedConcert = ConcertFactory.createConcert();
            concertStore.save(savedConcert);

            Optional<Concert> foundConcert = concertStore
                    .findById(savedConcert.getId());

            assertThat(foundConcert)
                    .get()
                    .isNotSameAs(savedConcert);
        }
    }

    @Nested
    public class ConcertEventStoreAllEvents {

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void allEventsReturnsAllEventsForAllSavedAggregatesInGlobalEventSequenceOrder(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert firstConcert = ConcertFactory.createConcertWithArtist("First Concert");
            List<ConcertEvent> expectedEvents = new ArrayList<>(firstConcert.uncommittedEvents().toList());
            concertStore.save(firstConcert);
            Concert originalConcert = ConcertFactory.createConcertWithArtist("Original Concert, to be Rescheduled");
            expectedEvents.addAll(originalConcert.uncommittedEvents().toList());
            concertStore.save(originalConcert);
            Concert rescheduledConcert = concertStore.findById(originalConcert.getId()).orElseThrow();
            rescheduledConcert.rescheduleTo(LocalDateTime.now(), LocalTime.now().minusHours(1));
            expectedEvents.addAll(rescheduledConcert.uncommittedEvents().toList());
            concertStore.save(rescheduledConcert);

            Stream<ConcertEvent> concertEventStream =
                    concertStore.allEvents();

            assertThat(concertEventStream)
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("eventSequence")
                    .containsExactlyElementsOf(expectedEvents);
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void noEventsReturnedForAllEventsAfterWhenEventStoreIsEmpty(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Stream<ConcertEvent> newEvents = concertStore.allEventsAfter(0L);

            assertThat(newEvents)
                    .as("Event Store is empty, so should not return any events")
                    .isEmpty();
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void oneEventReturnForOneEventInStoreAskingForEventsAfterZero(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId = ConcertId.createRandom();
            concertStore.save(concertId,
                              MakeEvents.with()
                                        .concertScheduled(concertId)
                                        .stream());

            Stream<ConcertEvent> newEvents = concertStore.allEventsAfter(0L);

            assertThat(newEvents)
                    .as("Event Store has 1 ConcertScheduled event, so should return that event")
                    .extracting(ConcertEvent::concertId)
                    .containsExactly(concertId);
        }


        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void oneOfTwoEventsReturnedFromStoreAskingForEventsAfterOne(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId1 = ConcertId.createRandom();
            ConcertId concertId2 = ConcertId.createRandom();
            long eventSequenceStoredForFirstEvent =
                    concertStore.save(concertId1,
                                      MakeEvents.with()
                                                .concertScheduled(concertId1)
                                                .stream())
                                .findFirst().orElseThrow().eventSequence();
            concertStore.save(concertId2,
                              MakeEvents.with()
                                        .concertScheduled(concertId2)
                                        .stream());

            Stream<ConcertEvent> newEvents = concertStore.allEventsAfter(eventSequenceStoredForFirstEvent);

            assertThat(newEvents)
                    .as("Event Store has 2 ConcertScheduled events, but should only return the second event")
                    .extracting(ConcertEvent::concertId)
                    .containsExactly(concertId2);
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void noEventsReturnedForAllEventsAfterTheMaxGlobalEventSequence(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Stream<ConcertEvent> newEvents = concertStore.allEventsAfter(2L);

            assertThat(newEvents)
                    .as("Max GES is 2, so asking for events AFTER 2 should not return any")
                    .isEmpty();
        }

    }

    @Nested
    public class ConcertEventStoreEventsForAggregate {

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void emptyListReturnedForUnknownAggregateId(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId unknownConcertId = ConcertId.createRandom();
            List<ConcertEvent> concertEvents = concertStore.eventsForAggregate(unknownConcertId);

            assertThat(concertEvents)
                    .isEmpty();
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void exactlyAllEventsForSpecifiedConcertAggregateAreReturned(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId = ConcertId.createRandom();
            Concert concert = Concert.schedule(concertId, "artist", 30, LocalDateTime.now(), LocalTime.now().minusHours(1), 100, 8);
            concert.rescheduleTo(LocalDateTime.now(), LocalTime.now().minusHours(1));
            concert.rescheduleTo(LocalDateTime.now().plusMonths(2), LocalTime.now().minusHours(1));
            concert.sellTicketsTo(CustomerId.createRandom(), 4);
            concert.sellTicketsTo(CustomerId.createRandom(), 2);
            concert.sellTicketsTo(CustomerId.createRandom(), 4);
            concert.sellTicketsTo(CustomerId.createRandom(), 1);
            Stream<ConcertEvent> allEventsForConcert = concert.uncommittedEvents();
            concertStore.save(concert);

            List<ConcertEvent> eventsForAggregate =
                    concertStore.eventsForAggregate(concertId);

            assertThat(eventsForAggregate)
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("eventSequence")
                    .containsExactlyElementsOf(allEventsForConcert.toList());
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void eventSequenceAssignedWhenReturnedFromSave(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId = ConcertId.createRandom();
            List<ConcertEvent> oneEvent = MakeEvents.withNullEventSequences()
                                                      .concertScheduled(concertId)
                                                      .list();

            Stream<ConcertEvent> savedEvents = concertStore.save(concertId, oneEvent.stream());

            assertThat(savedEvents)
                    .extracting(ConcertEvent::eventSequence)
                    .as("1 new event was saved in an empty store, so expected last Event Sequence returned to be 42 (baseline se")
                    .isNotNull();
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("dev.ted.jitterticket.eventsourced.application.EventStoreTest#concertEventStoreSupplier")
        public void lastGlobalEventSequenceSavedReturnedAfterMultipleSaves(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertIdFirst = ConcertId.createRandom();
            Stream<ConcertEvent> onePreExistingEvent = MakeEvents.withNullEventSequences().concertScheduled().stream();
            concertStore.save(concertIdFirst, onePreExistingEvent);
            ConcertId concertIdSecond = ConcertId.createRandom();
            Stream<ConcertEvent> twoNewEvents = MakeEvents
                    .with()
                    .concertScheduled(concertIdSecond,
                                      concert -> concert.ticketsSold(2))
                    .stream();

            List<ConcertEvent> savedEvents =
                    concertStore.save(concertIdSecond, twoNewEvents)
                                .toList();

            assertThat(savedEvents.getLast().eventSequence())
                    .as("2 new events were saved in a store containing 1 event, so expected last event saved to have a larger event sequence")
                    .isGreaterThan(savedEvents.getFirst().eventSequence())
            ;
        }
    }

    @ParameterizedClass(name = "Using {0} Storage")
    @MethodSource("customerEventStoreSupplier")
    @Nested
    class CustomerEventStoreTest {
        @TempDir
        static Path tempDir;

        static Stream<Arguments> customerEventStoreSupplier() throws IOException {
            Path tempFile = Files.createTempFile(tempDir, "test", ".csv");

            return Stream.of(
                    Arguments.of(Named.of("In-Memory", InMemoryEventStore.forCustomers())),
                    Arguments.of(Named.of("CSV ArrayList", CsvStringsEventStore.forCustomers(new ArrayListStringsReaderAppender()))),
                    Arguments.of(Named.of("CSV File", CsvStringsEventStore.forCustomers(new CsvReaderAppender(tempFile))))
            );
        }

        @Parameter
        EventStore<CustomerId, CustomerEvent, Customer> customerStore;

        @Test
        public void eventStoreCanStoreCustomers() {
            Customer savedCustomer = Customer.register(CustomerId.createRandom(), "name", "email@example.com");
            customerStore.save(savedCustomer);

            Optional<Customer> foundCustomer = customerStore.findById(savedCustomer.getId());

            assertThat(foundCustomer)
                    .isPresent()
                    .get()
                    .isNotSameAs(savedCustomer);
        }

        @Test
        void exactlyAllEventsForSpecifiedCustomerAggregateAreReturned() {
            CustomerId customerId = CustomerId.createRandom();
            Customer customer = Customer.register(customerId, "customer name", "customer@example.com");
            Concert concert = ConcertFactory.createConcert();
            customer.purchaseTickets(concert, TicketOrderId.createRandom(), 4);
            customer.purchaseTickets(concert, TicketOrderId.createRandom(), 2);
            Stream<CustomerEvent> allEventsForCustomer = customer.uncommittedEvents();
            customerStore.save(customer);

            List<CustomerEvent> eventsForAggregate = customerStore.eventsForAggregate(customerId);

            assertThat(eventsForAggregate)
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("eventSequence")
                    .containsExactlyElementsOf(allEventsForCustomer.toList());
        }
    }
}
