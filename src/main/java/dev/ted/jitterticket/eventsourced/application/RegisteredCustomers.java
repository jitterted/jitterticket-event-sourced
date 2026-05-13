package dev.ted.jitterticket.eventsourced.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class RegisteredCustomers implements ProjectionDelta {
    private final List<RegisteredCustomer> registeredCustomers = new ArrayList<>();

    public RegisteredCustomers() {
    }

    private RegisteredCustomers(RegisteredCustomer... registeredCustomers) {
        add(registeredCustomers);
    }

    public static RegisteredCustomers createForTestWith(RegisteredCustomer... registeredCustomers) {
        return new RegisteredCustomers(registeredCustomers);
    }

    private RegisteredCustomers(List<RegisteredCustomer> registeredCustomers) {
        add(registeredCustomers);
    }

    public static RegisteredCustomers createForTestWith(List<RegisteredCustomer> registeredCustomers) {
        return new RegisteredCustomers(registeredCustomers);
    }

    RegisteredCustomers withNew(List<RegisteredCustomer> newlyRegisteredCustomers) {
        RegisteredCustomers newState = createForTestWith(asList());
        newState.add(newlyRegisteredCustomers);
        return newState;
    }

    public RegisteredCustomers withNew(RegisteredCustomers delta) {
        return withNew(delta.asList());
    }

    void add(List<RegisteredCustomer> newlyRegisteredCustomers) {
        registeredCustomers.addAll(newlyRegisteredCustomers);
    }

    public void add(RegisteredCustomer... newlyRegisteredCustomers) {
        registeredCustomers.addAll(Arrays.asList(newlyRegisteredCustomers));
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
    public boolean isEmpty() {
        return registeredCustomers.isEmpty();
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

}
