package dev.ted.jitterticket.eventsourced.application;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class AllRegisteredCustomers {
    private final List<RegisteredCustomer> registeredCustomers = new ArrayList<>();
    private Checkpoint checkpoint;

    public AllRegisteredCustomers() {
        checkpoint = Checkpoint.INITIAL;
    }

    private AllRegisteredCustomers(List<RegisteredCustomer> registeredCustomers, Checkpoint checkpoint) {
        this.registeredCustomers.addAll(registeredCustomers);
        this.checkpoint = checkpoint;
    }

    static AllRegisteredCustomers copyOf(AllRegisteredCustomers initialState) {
        return new AllRegisteredCustomers(initialState.asList(), initialState.checkpoint);
    }

    void add(List<RegisteredCustomer> newlyRegisteredCustomers, Checkpoint checkpoint) {
        registeredCustomers.addAll(newlyRegisteredCustomers);
        this.checkpoint = checkpoint;
    }

    public void add(RegisteredCustomer registeredCustomer, Checkpoint checkpoint) {
        registeredCustomers.add(registeredCustomer);
        this.checkpoint = checkpoint;
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

    public Checkpoint checkpoint() {
        return checkpoint;
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
                .add("checkpoint=" + checkpoint)
                .toString();
    }
}
