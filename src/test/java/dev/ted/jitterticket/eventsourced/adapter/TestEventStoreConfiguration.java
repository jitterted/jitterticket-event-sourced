package dev.ted.jitterticket.eventsourced.adapter;

import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;

@TestConfiguration
public class TestEventStoreConfiguration {

    @Bean
    public EventStore<ConcertId, ConcertEvent, Concert> concertStore() throws IOException {
        return InMemoryEventStore.forConcerts();
    }

    @Bean
    public EventStore<CustomerId, CustomerEvent, Customer> customerStore() throws IOException {
        return InMemoryEventStore.forCustomers();
    }

}
