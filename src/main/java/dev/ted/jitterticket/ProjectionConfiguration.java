package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.application.AvailableConcerts;
import dev.ted.jitterticket.eventsourced.application.AvailableConcertsDelta;
import dev.ted.jitterticket.eventsourced.application.AvailableConcertsProjector;
import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjectionMediator;
import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjector;
import dev.ted.jitterticket.eventsourced.application.MemoryAvailableConcertsProjectionPersistence;
import dev.ted.jitterticket.eventsourced.application.MemoryRegisteredCustomersProjectionPersistence;
import dev.ted.jitterticket.eventsourced.application.ProjectionCoordinator;
import dev.ted.jitterticket.eventsourced.application.RegisteredCustomers;
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
public class ProjectionConfiguration {

    @Bean
    ProjectionCoordinator<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> concertProjectionCoordinator(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        return new ProjectionCoordinator<>(new AvailableConcertsProjector(),
                                           new MemoryAvailableConcertsProjectionPersistence(),
                                           concertStore);
    }

    @Bean
    ProjectionCoordinator<CustomerEvent, RegisteredCustomers, RegisteredCustomers> registeredCustomersProjectionCoordinator(
            EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        return new ProjectionCoordinator<>(new RegisteredCustomersProjector(),
                                           new MemoryRegisteredCustomersProjectionPersistence(),
                                           customerStore);
    }

    @Bean
    ConcertSalesProjectionMediator concertSalesProjectionMediator(
            EventStore<ConcertId, ConcertEvent, Concert> concertStore,
            ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        return new ConcertSalesProjectionMediator(
                new ConcertSalesProjector(),
                concertStore,
                concertSalesProjectionRepository);
    }

    @Bean
    ProjectionChoices projectionChoices(
            EventStore<ConcertId, ConcertEvent, Concert> concertStore,
            EventStore<CustomerId, CustomerEvent, Customer> customerStore,
            ProjectionCoordinator<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> concertProjection,
            ProjectionCoordinator<CustomerEvent, RegisteredCustomers, RegisteredCustomers> registeredCustomersProjection
    ) {
        return new ProjectionChoices(Map.of(
                "concerts", new ConcertProjectionChoice(concertStore, concertProjection),
                "customers", new CustomerProjectionChoice(customerStore, registeredCustomersProjection)
        ));
    }

}
