package dev.ted.jitterticket.eventsourced.application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Stream;

public class NewlyRegisteredCustomers implements ProjectionDelta {
    private final List<RegisteredCustomer> registeredCustomers = new ArrayList<>();
    private Checkpoint checkpoint;

    public NewlyRegisteredCustomers() {
    }

    private NewlyRegisteredCustomers(Checkpoint checkpoint, RegisteredCustomer... registeredCustomers) {
        add(registeredCustomers);
    }

    public static NewlyRegisteredCustomers createForTestWith(Checkpoint checkpoint, RegisteredCustomer... registeredCustomers) {
        return new NewlyRegisteredCustomers(checkpoint, registeredCustomers);
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

    public void updateCheckpointTo(Checkpoint checkpoint) {
        this.checkpoint = checkpoint;
    }

    public Checkpoint checkpoint() {
        return checkpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NewlyRegisteredCustomers that = (NewlyRegisteredCustomers) o;
        return registeredCustomers.equals(that.registeredCustomers);
    }

    @Override
    public int hashCode() {
        return registeredCustomers.hashCode();
    }

    @Override
    public String toString() {
        if (registeredCustomers.isEmpty()) {
            return NewlyRegisteredCustomers.class.getSimpleName() + "[empty]";
        }
        return new StringJoiner(", ", NewlyRegisteredCustomers.class.getSimpleName() + "[", "]")
                .add("registeredCustomers=" + registeredCustomers)
                .add("checkpoint=" + checkpoint)
                .toString();
    }
}
