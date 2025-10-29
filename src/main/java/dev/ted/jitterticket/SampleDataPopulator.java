package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.application.RegisteredCustomersProjector;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.stream.Stream;

import static dev.ted.jitterticket.EventStoreConfiguration.SONIC_WAVES_CONCERT_ID;

@Component
public class SampleDataPopulator implements ApplicationRunner {

    private final EventStore<CustomerId, CustomerEvent, Customer> customerStore;
    private final RegisteredCustomersProjector registeredCustomersProjector;

    public SampleDataPopulator(EventStore<CustomerId, CustomerEvent, Customer> customerStore, RegisteredCustomersProjector registeredCustomersProjector) {
        this.customerStore = customerStore;
        this.registeredCustomersProjector = registeredCustomersProjector;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // don't add sample data if the store already has CUSTOMER data
        if (registeredCustomersProjector.allCustomers().findAny().isPresent()) {
            return;
        }

        CustomerId firstCustomerId = new CustomerId(UUID.fromString("68f5b2c2-d70d-4992-ad78-c94809ae9a6a"));
        customerStore.save(Customer.register(
                firstCustomerId,
                "First Customer", "first@example.com"));
        customerStore.save(Customer.register(
                new CustomerId(UUID.fromString("123e4567-e89b-42d3-a456-556642440000")),
                "Another Customer", "another@example.com"
        ));
        customerStore.save(firstCustomerId,
                           Stream.of(new TicketsPurchased(firstCustomerId, 1, TicketOrderId.createRandom(), SONIC_WAVES_CONCERT_ID, 3, 150)));

    }
}
