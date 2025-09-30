package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.ArrayListStringsReaderAppender;
import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvReaderAppender;
import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvStringsEventStore;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.*;
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

import static org.assertj.core.api.Assertions.assertThat;

class EventStoreTest {

    @Nested
    class ConcertEventStoreTest {
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

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("concertEventStoreSupplier")
        void findByIdForNonExistingConcertReturnsEmptyOptional(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId nonExistentConcertId = ConcertId.createRandom();
            assertThat(concertStore.findById(nonExistentConcertId))
                    .as("Should not be able to find a non-existent Concert by ID")
                    .isEmpty();
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("concertEventStoreSupplier")
        void findByIdReturnsSavedConcert(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
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
        @MethodSource("concertEventStoreSupplier")
        void findByIdReturnsDifferentInstanceOfConcert(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert savedConcert = ConcertFactory.createConcert();
            concertStore.save(savedConcert);

            Optional<Concert> foundConcert = concertStore.findById(savedConcert.getId());

            assertThat(foundConcert)
                    .get()
                    .isNotSameAs(savedConcert);
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("concertEventStoreSupplier")
        void eventStoreReturnsAllEventsAcrossAllSavedAggregates(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
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

            Stream<ConcertEvent> concertEventStream = concertStore.allEvents();

            assertThat(concertEventStream)
                    .containsExactlyInAnyOrderElementsOf(expectedEvents);
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("concertEventStoreSupplier")
        void emptyListReturnedForUnknownAggregateId(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId unknownConcertId = ConcertId.createRandom();
            List<ConcertEvent> concertEvents = concertStore.eventsForAggregate(unknownConcertId);

            assertThat(concertEvents)
                    .isEmpty();
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("concertEventStoreSupplier")
        void exactlyAllEventsForSpecifiedConcertAggregateAreReturned(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
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
                    .containsExactlyElementsOf(allEventsForConcert.toList());
        }

        @ParameterizedTest(name = "Using {0} Storage")
        @MethodSource("concertEventStoreSupplier")
        void savingEventsDirectlyStoresThemCorrectly(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
            ConcertId concertId = ConcertId.createRandom();
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 4, 22, 19, 0);
            LocalTime originalDoorsTime = LocalTime.of(18, 0);
            ConcertScheduled concertScheduled = new ConcertScheduled(
                    concertId, 0, "Headliner", 45,
                    originalShowDateTime, originalDoorsTime,
                    150, 8);
            TicketsSold ticketsSold = new TicketsSold(concertId, 0, 4, 4 * 45);
            ConcertRescheduled concertRescheduled = new ConcertRescheduled(
                    concertId, 0, originalShowDateTime.plusMonths(2).plusHours(1),
                    originalDoorsTime.plusHours(1));

            concertStore.save(concertId, Stream.of(concertScheduled, ticketsSold, concertRescheduled));

            assertThat(concertStore.eventsForAggregate(concertId))
                    .containsExactly(concertScheduled, ticketsSold, concertRescheduled);
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
        void eventStoreCanStoreCustomers() {
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
                    .containsExactlyElementsOf(allEventsForCustomer.toList());
        }
    }
}
