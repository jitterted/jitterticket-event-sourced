package dev.ted.jitterticket.eventsourced.adapter;

import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.PurchaseTicketsUseCase;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventviewer.adapter.in.web.ConcertProjectionChoice;
import dev.ted.jitterticket.eventviewer.adapter.in.web.CustomerProjectionChoice;
import dev.ted.jitterticket.eventviewer.adapter.in.web.ProjectionChoices;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.util.Map;

@TestConfiguration
public class TixTestConfiguration {

    @Bean
    public EventStore<ConcertId, ConcertEvent, Concert> concertStore() throws IOException {
        return InMemoryEventStore.forConcerts();
    }

    @Bean
    public EventStore<CustomerId, CustomerEvent, Customer> customerStore() throws IOException {
        return InMemoryEventStore.forCustomers();
    }

    @Bean
    public ConcertSummaryProjector concertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new ConcertSummaryProjector(concertStore);
    }

    @Bean
    public ProjectionChoices projectionChoices(
            EventStore<ConcertId, ConcertEvent, Concert> concertStore,
            EventStore<CustomerId, CustomerEvent, Customer> customerStore
    ) {
        return new ProjectionChoices(Map.of(
                "concerts", new ConcertProjectionChoice(concertStore),
                "customers", new CustomerProjectionChoice(customerStore)
        ));
    }

    @Bean
    PurchaseTicketsUseCase purchaseTicketsUseCase(EventStore<CustomerId, CustomerEvent, Customer> customerStore,
                                                  EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new PurchaseTicketsUseCase(concertStore, customerStore);
    }

}
