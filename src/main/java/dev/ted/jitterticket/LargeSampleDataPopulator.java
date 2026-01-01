package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.EventDboRepository;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//@Component
public class LargeSampleDataPopulator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LargeSampleDataPopulator.class);

    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;
    private final EventDboRepository eventDboRepository;
    private final ConcertSalesProjectionRepository concertSalesProjectionRepository;
    private final Random random = new Random();

    private static final String[] ARTISTS = {
            "The Electric Vibrations", "Neon Dreams", "Solar Flare", "Midnight Pulse", "Acoustic Echo",
            "Quantum Harmony", "The Velvet Rhythms", "Lunar Shadows", "Cyber Soul", "Infinite Loop",
            "Starlight Symphony", "Bass Drop Theory", "The Echo Chamber", "Golden Hour", "Silver Lining",
            "Desert Wind", "Ocean Breeze", "Mountain Peak", "Urban Jungle", "Forest Whisper"
    };

    private static final String[] FIRST_NAMES = {
            "Alice", "Bob", "Charlie", "Diana", "Edward", "Fiona", "George", "Hannah", "Ian", "Julia",
            "Kevin", "Laura", "Michael", "Nina", "Oscar", "Paula", "Quinn", "Rose", "Steven", "Tanya"
    };

    private static final String[] LAST_NAMES = {
            "Adams", "Baker", "Clark", "Davis", "Evans", "Frank", "Ghosh", "Hills", "Irwin", "Jones",
            "Klein", "Lopez", "Mason", "Nolan", "Owen", "Perez", "Quinn", "Ross", "Smith", "Tyler"
    };

    public LargeSampleDataPopulator(EventStore<CustomerId, CustomerEvent, Customer> customerStore,
                                    EventStore<ConcertId, ConcertEvent, Concert> concertStore,
                                    EventDboRepository eventDboRepository,
                                    ConcertSalesProjectionRepository concertSalesProjectionRepository) {
        this.customerStore = customerStore;
        this.concertStore = concertStore;
        this.eventDboRepository = eventDboRepository;
        this.concertSalesProjectionRepository = concertSalesProjectionRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        eventDboRepository.deleteAll();
        concertSalesProjectionRepository.deleteAll();
        log.info("Cleared existing data, starting population of large sample dataset");

        List<Customer> customers = createCustomerObjects(1000);
        log.info("Created {} customer objects", customers.size());
        List<Concert> concerts = createConcertObjects(100);
        log.info("Created {} concert objects", concerts.size());

        log.info("Starting ticket sales processing for {} customers and {} concerts", customers.size(), concerts.size());
        for (Concert concert : concerts) {
            for (Customer customer : customers) {
                int quantity = random.nextBoolean() ? 2 : 4;
                concert.sellTicketsTo(customer.getId(), quantity);
                customer.purchaseTickets(concert, TicketOrderId.createRandom(), quantity);
            }
        }
        log.info("Completed ticket sales processing");

        log.info("Saving concerts and customers to event store");
        concerts.forEach(concertStore::save);
        customers.forEach(customerStore::save);
        log.info("Successfully populated database with {} concerts and {} customers", concerts.size(), customers.size());
    }

    private List<Customer> createCustomerObjects(int count) {
        List<Customer> customers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String firstName = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
            String lastName = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
            String name = firstName + " " + lastName + " " + i;
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@example.com";
            customers.add(Customer.register(CustomerId.createRandom(), name, email));
        }
        return customers;
    }

    private List<Concert> createConcertObjects(int count) {
        List<Concert> concerts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String artist = ARTISTS[random.nextInt(ARTISTS.length)] + " " + i;
            int price = 20 + random.nextInt(180);
            int capacity = 5000 + random.nextInt(5000);
            concerts.add(Concert.schedule(
                    ConcertId.createRandom(),
                    artist,
                    price,
                    daysFromNowAt(10 + random.nextInt(100), 19, 0),
                    LocalTime.of(18, 0),
                    capacity,
                    10
            ));
        }
        return concerts;
    }

    private static LocalDateTime daysFromNowAt(long days, int hour, int minute) {
        return LocalDateTime.now()
                .truncatedTo(ChronoUnit.DAYS)
                .plusDays(days)
                .withHour(hour)
                .withMinute(minute);
    }
}
