package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import dev.ted.jitterticket.eventsourced.application.PurchaseTicketsUseCase;
import dev.ted.jitterticket.eventsourced.application.RegisteredCustomersProjector;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
class TixConfiguration {

    @Bean
    PurchaseTicketsUseCase purchaseTicketsUseCase(EventStore<CustomerId, CustomerEvent, Customer> customerStore,
                                                  EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new PurchaseTicketsUseCase(concertStore, customerStore);
    }

    @Bean
    ConcertSummaryProjector concertProjector(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new ConcertSummaryProjector(concertStore);
    }

    @Bean
    RegisteredCustomersProjector RegisteredCustomersProjector(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        return new RegisteredCustomersProjector(customerStore);
    }

    @Bean
    ProjectionChoices projectionChoices(
            EventStore<ConcertId, ConcertEvent, Concert> concertStore,
            EventStore<CustomerId, CustomerEvent, Customer> customerStore
    ) {
        return new ProjectionChoices(Map.of(
                "concerts", new ConcertProjectionChoice(concertStore),
                "customers", new CustomerProjectionChoice(customerStore)
        ));
    }

}
