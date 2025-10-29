package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvReaderAppender;
import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvStringsEventStore;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.EventDboRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.JdbcEventStore;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Configuration
public class EventStoreConfiguration {

    public enum StoreType {
        IN_MEMORY, CSV, JDBC
    }

    @Bean
    public EventStore<CustomerId, CustomerEvent, Customer> customerStore(
            @Value("${event.store.csv.directory}") String eventsDirectory,
            @Value("${event.store.type}") StoreType storeType,
            EventDboRepository eventDboRepository) throws IOException {
        return switch (storeType) {
            case IN_MEMORY -> InMemoryEventStore.forCustomers();
            case CSV -> csvCustomerEventStoreIn(eventsDirectory);
            case JDBC -> JdbcEventStore.forCustomers(eventDboRepository);
        };
    }

    private EventStore<CustomerId, CustomerEvent, Customer> csvCustomerEventStoreIn(String eventsDirectory) throws IOException {
        String eventsFilePath = eventsDirectory + File.separator + "customer-events.csv";
        return CsvStringsEventStore.forCustomers(new CsvReaderAppender(Path.of(eventsFilePath)));
    }

    @Bean
    public EventStore<ConcertId, ConcertEvent, Concert> concertStore(
            @Value("${event.store.csv.directory}") String eventsDirectory,
            @Value("${event.store.type}") StoreType storeType,
            EventDboRepository eventDboRepository) throws IOException {
        if (Strings.isBlank(eventsDirectory)) {
            throw new IllegalArgumentException("eventsDirectory (from events.directory) is empty");
        }
        return switch (storeType) {
            case IN_MEMORY -> InMemoryEventStore.forConcerts();
            case CSV -> csvConcertStoreIn(eventsDirectory);
            case JDBC -> JdbcEventStore.forConcerts(eventDboRepository);
        };
    }

    private static EventStore<ConcertId, ConcertEvent, Concert> csvConcertStoreIn(String eventsDirectory) throws IOException {
        String eventsFilePath = eventsDirectory + File.separator + "concert-events.csv";
        var concertStore = CsvStringsEventStore.forConcerts(
                new CsvReaderAppender(Path.of(eventsFilePath)));
        return concertStore;
    }


}
