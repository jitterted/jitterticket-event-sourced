package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.eventsourced.application.EventStoreTest;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class JdbcEventStoreTest extends DataJdbcContainerTest {

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

        protected EventStore<ConcertId, ConcertEvent, Concert> concertStore;
        private EventStoreTest.ConcertEventStoreFindById delegatedConcertEventStoreFindById;
        private EventStoreTest.ConcertEventStoreAllEvents delegatedConcertEventStoreAllEvents;
        private EventStoreTest.ConcertEventStoreEventsForAggregate delegatedConcertEventStoreEventsForAggregate;

        @BeforeEach
        void beforeEach() {
            @SuppressWarnings("InstantiationOfUtilityClass")
            EventStoreTest eventStoreTest = new EventStoreTest();
            // yes, the .new is valid Java syntax for instantiating a non-static inner class
            delegatedConcertEventStoreFindById = eventStoreTest.new ConcertEventStoreFindById();
            delegatedConcertEventStoreAllEvents = eventStoreTest.new ConcertEventStoreAllEvents();
            delegatedConcertEventStoreEventsForAggregate = eventStoreTest.new ConcertEventStoreEventsForAggregate();
            concertStore = concertStore();
        }

        @Test
        void findByIdForNonExistingConcertReturnsEmptyOptional() {
            delegatedConcertEventStoreFindById
                    .findByIdForNonExistingConcertReturnsEmptyOptional(concertStore);
        }

        @Test
        void findByIdReturnsSavedConcert() {
            delegatedConcertEventStoreFindById
                    .findByIdReturnsSavedConcert(concertStore);
        }

        @Test
        void findByIdReturnsDifferentInstanceOfConcert() {
            delegatedConcertEventStoreFindById
                    .findByIdReturnsDifferentInstanceOfConcert(concertStore);
        }

        @Test
        void allEventsReturnsAllEventsForAllSavedAggregatesInGlobalEventSequenceOrder() {
            delegatedConcertEventStoreAllEvents
                    .allEventsReturnsAllEventsForAllSavedAggregatesInGlobalEventSequenceOrder(concertStore);
        }

        @Test
        void noEventsReturnedForAllEventsAfterWhenEventStoreIsEmpty() {
            delegatedConcertEventStoreAllEvents
                    .noEventsReturnedForAllEventsAfterWhenEventStoreIsEmpty(concertStore);
        }

        @Test
        void oneEventReturnedForOneEventInStoreAskingForEventsAfterZero() {
            delegatedConcertEventStoreAllEvents
                    .oneEventReturnForOneEventInStoreAskingForEventsAfterZero(concertStore);
        }

        @Test
        void oneOfTwoEventsReturnedFromStoreAskingForEventsAfterOne() {
            delegatedConcertEventStoreAllEvents
                    .oneOfTwoEventsReturnedFromStoreAskingForEventsAfterOne(concertStore);
        }

        @Test
        void noEventsReturnedForAllEventsAfterTheMaxGlobalEventSequence() {
            delegatedConcertEventStoreAllEvents
                    .noEventsReturnedForAllEventsAfterTheMaxGlobalEventSequence(concertStore);
        }

        @Test
        void emptyListReturnedForUnknownAggregateId() {
            delegatedConcertEventStoreEventsForAggregate
                    .emptyListReturnedForUnknownAggregateId(concertStore);
        }

        @Test
        void exactlyAllEventsForSpecifiedConcertAggregateAreReturned() {
            delegatedConcertEventStoreEventsForAggregate
                    .exactlyAllEventsForSpecifiedConcertAggregateAreReturned(concertStore);
        }

        @Test
        void lastGlobalEventSequenceSavedReturnedFromSave() {
            delegatedConcertEventStoreEventsForAggregate
                    .eventSequenceAssignedWhenReturnedFromSave(concertStore);
        }

        @Test
        void lastGlobalEventSequenceSavedReturnedAfterMultipleSaves() {
            delegatedConcertEventStoreEventsForAggregate
                    .lastGlobalEventSequenceSavedReturnedAfterMultipleSaves(concertStore);
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
                    .usingRecursiveFieldByFieldElementComparatorIgnoringFields("eventSequence")
                    .containsExactlyElementsOf(allEventsForCustomer.toList());
        }
    }
}
