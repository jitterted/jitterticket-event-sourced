package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.application.*;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventviewer.adapter.in.web.ConcertProjectionChoice;
import dev.ted.jitterticket.eventviewer.adapter.in.web.NewCustomerProjectionChoice;
import dev.ted.jitterticket.eventviewer.adapter.in.web.ProjectionChoices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class ProjectionConfiguration {

    @Bean
    ProjectionCoordinator<ConcertEvent, AvailableConcerts, AvailableConcertsDelta>
    availableConcertsProjectionCoordinator(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        System.gc();
        return new ProjectionCoordinator<>(new AvailableConcertsProjector(),
                                           new MemoryAvailableConcertsProjectionPersistence(),
                                           concertStore);
    }

//    @Bean
//    ProjectionCoordinator<CustomerEvent, RegisteredCustomers, RegisteredCustomers>
//    registeredCustomersProjectionCoordinator(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
//        return new ProjectionCoordinator<>(new RegisteredCustomersProjector(),
//                                           new MemoryRegisteredCustomersProjectionPersistence(),
//                                           customerStore);
//    }

    @Bean
    NewProjectionCoordinator<AllRegisteredCustomers, NewlyRegisteredCustomers>
    newRegisteredCustomersProjectionCoordinator(EventStore<CustomerId, CustomerEvent, Customer> customerStore) {
        return new NewProjectionCoordinator<>(
                new NewMemoryRegisteredCustomersProjectionPersistence(),
                customerStore);
    }


    @Bean
    ProjectionCoordinator<ConcertEvent, ScheduledConcerts, ScheduledConcertsDelta>
    scheduledConcertsProjectionCoordinator(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        System.gc();
        return new ProjectionCoordinator<>(new ScheduledConcertsProjector(),
                                           new MemoryScheduledConcertsProjectionPersistence(),
                                           concertStore);
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
            EventStore<CustomerId, CustomerEvent, Customer> customerStore
//            , ProjectionCoordinator<CustomerEvent, RegisteredCustomers, RegisteredCustomers> registeredCustomersProjection
            ,NewProjectionCoordinator<AllRegisteredCustomers, NewlyRegisteredCustomers> newRegisteredCustomersProjection
    ) {
        ProjectionCoordinator<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> allConcertsProjection =
                new ProjectionCoordinator<>(new AllConcertsProjector(),
                                            new MemoryAvailableConcertsProjectionPersistence(),
                                            concertStore);
        return new ProjectionChoices(Map.of(
                "concerts", new ConcertProjectionChoice(concertStore, allConcertsProjection)
//                , "customers", new CustomerProjectionChoice(customerStore, registeredCustomersProjection)
                , "customers", new NewCustomerProjectionChoice(customerStore, newRegisteredCustomersProjection)
        ));
    }

}
