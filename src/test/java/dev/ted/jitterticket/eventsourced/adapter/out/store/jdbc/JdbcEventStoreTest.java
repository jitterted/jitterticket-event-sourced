package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.jdbc.DataJdbcTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@Tag("spring")
@DataJdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class JdbcEventStoreTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17.6");

    @Autowired
    EventDboRepository eventDboRepository;

    private EventStore<ConcertId, ConcertEvent, Concert> concertStore() {
        return JdbcEventStore.forConcerts(eventDboRepository);
    }

    private EventStore<CustomerId, CustomerEvent, Customer> customerStore() {
        return JdbcEventStore.forCustomers(eventDboRepository);
    }

    @Nested
    class EventStoreTests {
        @Test
        void onlyReturnEventsMatchingTypeOfEventStore() {
            var customerStore = customerStore();
            CustomerId onlyCustomerId = new CustomerId(UUID.fromString("68f5b2c2-d70d-4992-ad78-c94809ae9a6a"));
            customerStore.save(Customer.register(onlyCustomerId, "First Customer", "first@example.com"));
            var concertStore = concertStore();
            concertStore.save(ConcertFactory.createConcertWithArtist("Concert"));

            assertThat(customerStore.allEvents())
                    .as("Only one Customer event should be in the store")
                    .map(CustomerEvent::customerId)
                    .containsExactly(onlyCustomerId);
        }
    }

    @Nested
    class ConcertEventStoreTests {
        @Test
        void findByIdForNonExistingConcertReturnsEmptyOptional() {
            EventStore<ConcertId, ConcertEvent, Concert> concertStore = concertStore();
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId nonExistentConcertId = ConcertId.createRandom();

            assertThat(concertStore.findById(nonExistentConcertId))
                    .as("Should not be able to find a non-existent Concert by ID")
                    .isEmpty();
        }

        @Test
        void findByIdReturnsSavedConcert() {
            EventStore<ConcertId, ConcertEvent, Concert> concertStore = concertStore();
            ConcertId concertId = ConcertId.createRandom();
            Concert concert = Concert.schedule(concertId,
                    "Headliner",
                    99,
                    LocalDateTime.now(),
                    LocalTime.now().minusHours(1),
                    100,
                    4);

            concertStore.save(concert);

            assertThat(concertStore.findById(concertId))
                    .as("Should be able to find a saved Concert by its ConcertId")
                    .isPresent()
                    .get()
                    .extracting(Concert::artist)
                    .isEqualTo("Headliner");
        }

        @Test
        void findByIdReturnsDifferentInstanceOfConcert() {
            EventStore<ConcertId, ConcertEvent, Concert> store = concertStore();
            Concert savedConcert = ConcertFactory.createConcert();
            store.save(savedConcert);

            Optional<Concert> foundConcert = store.findById(savedConcert.getId());

            assertThat(foundConcert)
                    .get()
                    .isNotSameAs(savedConcert);
        }

        @Test
        void eventStoreReturnsAllEventsAcrossAllSavedAggregates() {
            EventStore<ConcertId, ConcertEvent, Concert> store = concertStore();
            Concert firstConcert = ConcertFactory.createConcertWithArtist("First Concert");
            List<ConcertEvent> expectedEvents = new ArrayList<>(firstConcert.uncommittedEvents().toList());
            store.save(firstConcert);

            Concert originalConcert = ConcertFactory.createConcertWithArtist("Original Concert, to be Rescheduled");
            expectedEvents.addAll(originalConcert.uncommittedEvents().toList());
            store.save(originalConcert);

            Concert rescheduledConcert = store.findById(originalConcert.getId()).orElseThrow();
            rescheduledConcert.rescheduleTo(LocalDateTime.now(), LocalTime.now().minusHours(1));
            expectedEvents.addAll(rescheduledConcert.uncommittedEvents().toList());
            store.save(rescheduledConcert);

            Stream<ConcertEvent> concertEventStream = store.allEvents();

            assertThat(concertEventStream)
                    .containsExactlyInAnyOrderElementsOf(expectedEvents);
        }

        @Test
        void emptyListReturnedForUnknownAggregateId() {
            EventStore<ConcertId, ConcertEvent, Concert> store = concertStore();
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            store.save(existingConcert);
            ConcertId unknownConcertId = ConcertId.createRandom();
            List<ConcertEvent> concertEvents = store.eventsForAggregate(unknownConcertId);

            assertThat(concertEvents).isEmpty();
        }

        @Test
        void exactlyAllEventsForSpecifiedConcertAggregateAreReturned() {
            EventStore<ConcertId, ConcertEvent, Concert> store = concertStore();
            ConcertId concertId = ConcertId.createRandom();
            Concert concert = Concert.schedule(concertId, "artist", 30,
                                              LocalDateTime.now(), LocalTime.now().minusHours(1), 100, 8);
            concert.rescheduleTo(LocalDateTime.now(), LocalTime.now().minusHours(1));
            concert.rescheduleTo(LocalDateTime.now().plusMonths(2), LocalTime.now().minusHours(1));
            concert.sellTicketsTo(CustomerId.createRandom(), 4);
            concert.sellTicketsTo(CustomerId.createRandom(), 2);
            concert.sellTicketsTo(CustomerId.createRandom(), 4);
            concert.sellTicketsTo(CustomerId.createRandom(), 1);
            Stream<ConcertEvent> allEventsForConcert = concert.uncommittedEvents();
            store.save(concert);

            List<ConcertEvent> eventsForAggregate = store.eventsForAggregate(concertId);

            assertThat(eventsForAggregate)
                    .containsExactlyElementsOf(allEventsForConcert.toList());
        }

        @Test
        void savingEventsDirectlyStoresThemCorrectly() {
            EventStore<ConcertId, ConcertEvent, Concert> concertStore = concertStore();
            ConcertId concertId = ConcertId.createRandom();
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 4, 22, 19, 0);
            LocalTime originalDoorsTime = LocalTime.of(18, 0);
            ConcertScheduled concertScheduled = new ConcertScheduled(
                    concertId, 1, "Headliner", 45,
                    originalShowDateTime, originalDoorsTime,
                    150, 8);
            TicketsSold ticketsSold = new TicketsSold(concertId, 2, 4, 4 * 45);
            ConcertRescheduled concertRescheduled = new ConcertRescheduled(
                    concertId, 3, originalShowDateTime.plusMonths(2).plusHours(1),
                    originalDoorsTime.plusHours(1));

            concertStore.save(concertId, Stream.of(concertScheduled, ticketsSold, concertRescheduled));

            assertThat(concertStore.eventsForAggregate(concertId))
                    .containsExactly(concertScheduled, ticketsSold, concertRescheduled);
        }
    }

    @Nested
    class CustomerEventStoreTests {
        @Test
        void eventStoreCanStoreCustomers() {
            EventStore<CustomerId, CustomerEvent, Customer> store = customerStore();
            Customer savedCustomer = Customer.register(CustomerId.createRandom(), "name", "email@example.com");
            store.save(savedCustomer);

            Optional<Customer> foundCustomer = store.findById(savedCustomer.getId());

            assertThat(foundCustomer)
                    .isPresent()
                    .get()
                    .isNotSameAs(savedCustomer);
        }

        @Test
        void exactlyAllEventsForSpecifiedCustomerAggregateAreReturned() {
            EventStore<CustomerId, CustomerEvent, Customer> store = customerStore();
            CustomerId customerId = CustomerId.createRandom();
            Customer customer = Customer.register(customerId, "customer name", "customer@example.com");
            Concert concert = ConcertFactory.createConcert();
            customer.purchaseTickets(concert, TicketOrderId.createRandom(), 4);
            customer.purchaseTickets(concert, TicketOrderId.createRandom(), 2);
            Stream<CustomerEvent> allEventsForCustomer = customer.uncommittedEvents();
            store.save(customer);

            List<CustomerEvent> eventsForAggregate = store.eventsForAggregate(customerId);

            assertThat(eventsForAggregate)
                    .containsExactlyElementsOf(allEventsForCustomer.toList());
        }
    }
}
