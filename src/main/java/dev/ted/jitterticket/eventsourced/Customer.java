package dev.ted.jitterticket.eventsourced;

public class Customer extends EventSourcedAggregate<CustomerEvent> {

    public static Customer register(String name, String email) {
        return new Customer();
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {

    }
}
