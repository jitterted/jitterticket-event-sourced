package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvFileEventStore;
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
import org.junit.jupiter.api.Test;
import org.lazyparams.LazyParams;
import org.lazyparams.showcase.TestInstance_Lifecycle_PER_METHOD_REPETITION;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class EventStoreTest {

    enum EventStoreType { InMemory, CSV_File;

        static EventStoreType pick() {
            return LazyParams.pickValue(
                    type -> "Using " + type + " Storage", values());
        }

        EventStore<CustomerId, CustomerEvent, Customer> forCustomers() {
            switch (this) {
                case InMemory: return InMemoryEventStore.forCustomers();
            }
            throw new Error("Store-type not implemented: " + name());
        }

        EventStore<ConcertId, ConcertEvent, Concert> forConcerts() {
            return switch (this) {
                case InMemory -> InMemoryEventStore.forConcerts();
                case CSV_File -> CsvFileEventStore.forConcerts();
            };
        }
    }

    @Nested
    class ConcertEventStoreTest {

        @Test
        void findByIdForNonExistingConcertReturnsEmptyOptional() {
            var concertStore = EventStoreType.pick().forConcerts();
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId nonExistentConcertId = ConcertId.createRandom();
            assertThat(concertStore.findById(nonExistentConcertId))
                    .as("Should not be able to find a non-existent Concert by ID")
                    .isEmpty();
        }

        @Test
        void findByIdReturnsSavedConcert() {
            var concertStore = EventStoreType.pick().forConcerts();
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

        @Test
        void findByIdReturnsDifferentInstanceOfConcert() {
            var concertStore = EventStoreType.pick().forConcerts();
            Concert savedConcert = ConcertFactory.createConcert();
            concertStore.save(savedConcert);

            Optional<Concert> foundConcert = concertStore.findById(savedConcert.getId());

            assertThat(foundConcert)
                    .get()
                    .isNotSameAs(savedConcert);
        }

        @Test
        void eventStoreReturnsAllEventsAcrossAllSavedAggregates() {
            var concertStore = EventStoreType.pick().forConcerts();
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

        @Test
        void emptyListReturnedForUnknownAggregateId() {
            var concertStore = EventStoreType.pick().forConcerts();
            Concert existingConcert = ConcertFactory.createConcertWithArtist("Existing Concert");
            concertStore.save(existingConcert);
            ConcertId unknownConcertId = ConcertId.createRandom();
            List<ConcertEvent> concertEvents = concertStore.eventsForAggregate(unknownConcertId);

            assertThat(concertEvents)
                    .isEmpty();
        }

        @Test
        void exactlyAllEventsForSpecifiedConcertAggregateAreReturned() {
            var concertStore = EventStoreType.pick().forConcerts();
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

        @Test
        void savingEventsDirectlyStoresThemCorrectly() {
            var concertStore = EventStoreType.pick().forConcerts();
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

    @TestInstance_Lifecycle_PER_METHOD_REPETITION
    @Nested
    class CustomerEventStoreTest {

        EventStore<CustomerId, CustomerEvent, Customer> customerStore = LazyParams
                .<EventStoreType>pickValue(
                        type -> "Using " + type + " Storage",
//                        EventStoreType.CSV_File,
                        EventStoreType.InMemory)
                .forCustomers();

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
