package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvReaderAppender;
import dev.ted.jitterticket.eventsourced.adapter.out.store.CsvStringsEventStore;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.EventDboRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.JdbcEventStore;
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

    @Bean
    public EventStore<CustomerId, CustomerEvent, Customer> customerStore(@Value("${events.directory}") String eventsDirectory, EventDboRepository eventDboRepository) throws IOException {
//        var customerStore = InMemoryEventStore.forCustomers();
//        var customerStore = csvCustomerEventStoreIn(eventsDirectory);

        return JdbcEventStore.forCustomers(eventDboRepository);
    }

    private EventStore<CustomerId, CustomerEvent, Customer> csvCustomerEventStoreIn(String eventsDirectory) throws IOException {
        String eventsFilePath = eventsDirectory + File.separator + "customer-events.csv";
        return CsvStringsEventStore.forCustomers(new CsvReaderAppender(Path.of(eventsFilePath)));
    }

    @Bean
    public EventStore<ConcertId, ConcertEvent, Concert> concertStore(@Value("${events.directory}") String eventsDirectory, EventDboRepository eventDboRepository) throws IOException {
        if (Strings.isBlank(eventsDirectory)) {
            throw new IllegalArgumentException("eventsDirectory (from events.directory) is empty");
        }
//        var concertStore = InMemoryEventStore.forConcerts();
//        var concertStore = csvConcertStoreIn(eventsDirectory);

        return JdbcEventStore.forConcerts(eventDboRepository);
    }

    private static EventStore<ConcertId, ConcertEvent, Concert> csvConcertStoreIn(String eventsDirectory) throws IOException {
        String eventsFilePath = eventsDirectory + File.separator + "concert-events.csv";
        var concertStore = CsvStringsEventStore.forConcerts(
                new CsvReaderAppender(Path.of(eventsFilePath)));
        return concertStore;
    }


}
