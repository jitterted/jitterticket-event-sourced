package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class RegisteredCustomers {
    private final List<RegisteredCustomer> registeredCustomers = new ArrayList<>();

    void add(List<RegisteredCustomer> newlyRegisteredCustomers) {
        registeredCustomers.addAll(newlyRegisteredCustomers);
    }

    public boolean hasData() {
        return !registeredCustomers.isEmpty();
    }

    public Stream<RegisteredCustomer> asStream() {
        return registeredCustomers.stream();
    }

    public List<RegisteredCustomer> asList() {
        return List.copyOf(registeredCustomers);
    }

    public record RegisteredCustomer(CustomerId customerId, String name) {}
}
