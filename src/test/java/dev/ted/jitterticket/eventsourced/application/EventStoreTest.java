package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.Customer;
import dev.ted.jitterticket.eventsourced.domain.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.CustomerId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class EventStoreTest {

    @Test
    void findByIdForNonExistingConcertReturnsEmptyOptional() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();

        ConcertId concertId = new ConcertId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"));
        assertThat(concertStore.findById(concertId))
                .as("Should not be able to find a non-existent Concert by ID")
                .isEmpty();
    }

    @Test
    void findByIdReturnsSavedConcert() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        ConcertId concertId = new ConcertId(UUID.randomUUID());
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
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        Concert savedConcert = createConcert();
        concertStore.save(savedConcert);

        Optional<Concert> foundConcert = concertStore.findById(savedConcert.getId());

        assertThat(foundConcert)
                .get()
                .isNotSameAs(savedConcert);
    }

    @Test
    @Disabled("dev.ted.jitterticket.eventsourced.application.EventStoreTest 4/3/25 12:41 — until Customer has ID assigned and event has ID")
    void eventStoreCanStoreCustomers() {
        EventStore<CustomerId, CustomerEvent, Customer> customerStore = EventStore.forCustomers();
        Customer savedCustomer = Customer.register(new CustomerId(UUID.randomUUID()), "name", "email@example.com");
        customerStore.save(savedCustomer);

        Optional<Customer> foundCustomer = customerStore.findById(savedCustomer.getId());

        assertThat(foundCustomer)
                .isPresent()
                .get()
                .isNotSameAs(savedCustomer);
    }

    private Concert createConcert() {
        return Concert.schedule(new ConcertId(UUID.randomUUID()),
                                "Headliner",
                                99,
                                LocalDateTime.now(),
                                LocalTime.now().minusHours(1),
                                100,
                                4
        );
    }
}
