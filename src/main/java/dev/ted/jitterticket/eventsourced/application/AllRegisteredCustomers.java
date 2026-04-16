package dev.ted.jitterticket.eventsourced.application;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class AllRegisteredCustomers {
    private final List<RegisteredCustomer> registeredCustomers = new ArrayList<>();

    public AllRegisteredCustomers() {
    }

    private AllRegisteredCustomers(List<RegisteredCustomer> registeredCustomers) {
        this.registeredCustomers.addAll(registeredCustomers);
    }

    static AllRegisteredCustomers copyOf(AllRegisteredCustomers initialState) {
        return new AllRegisteredCustomers(initialState.asList());
    }

    static AllRegisteredCustomers of(RegisteredCustomer registeredCustomer) {
        return new AllRegisteredCustomers(List.of(registeredCustomer));
    }

    public void add(List<RegisteredCustomer> newlyRegisteredCustomers) {
        registeredCustomers.addAll(newlyRegisteredCustomers);
    }

    public void add(RegisteredCustomer registeredCustomer) {
        registeredCustomers.add(registeredCustomer);
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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AllRegisteredCustomers that = (AllRegisteredCustomers) o;
        return registeredCustomers.equals(that.registeredCustomers);
    }

    @Override
    public int hashCode() {
        return registeredCustomers.hashCode();
    }

    @Override
    public String toString() {
        if (registeredCustomers.isEmpty()) {
            return AllRegisteredCustomers.class.getSimpleName() + "[empty]";
        }
        return new StringJoiner(", ", AllRegisteredCustomers.class.getSimpleName() + "[", "]")
                .add("registeredCustomers=" + registeredCustomers)
                .toString();
    }
}
