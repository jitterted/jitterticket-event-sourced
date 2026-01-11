package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class RegisteredCustomers {
    private final List<RegisteredCustomer> registeredCustomers = new ArrayList<>();

    public RegisteredCustomers() {
    }

    public RegisteredCustomers(RegisteredCustomer... registeredCustomers) {
        add(registeredCustomers);
    }

    public RegisteredCustomers(List<RegisteredCustomer> registeredCustomers) {
        add(registeredCustomers);
    }

    RegisteredCustomers withNew(List<RegisteredCustomer> newlyRegisteredCustomers) {
        RegisteredCustomers newState = new RegisteredCustomers(asList());
        newState.add(newlyRegisteredCustomers);
        return newState;
    }

    public RegisteredCustomers withNew(RegisteredCustomers delta) {
        return withNew(delta.asList());
    }

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

    public void add(RegisteredCustomer... newlyRegisteredCustomers) {
        registeredCustomers.addAll(Arrays.asList(newlyRegisteredCustomers));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RegisteredCustomers that = (RegisteredCustomers) o;
        return registeredCustomers.equals(that.registeredCustomers);
    }

    @Override
    public int hashCode() {
        return registeredCustomers.hashCode();
    }

    @Override
    public String toString() {
        if (registeredCustomers.isEmpty()) {
            return RegisteredCustomers.class.getSimpleName() + "[empty]";
        }
        return new StringJoiner(", ", RegisteredCustomers.class.getSimpleName() + "[", "]")
                .add("registeredCustomers=" + registeredCustomers)
                .toString();
    }

    public record RegisteredCustomer(CustomerId customerId, String name) {}
}
