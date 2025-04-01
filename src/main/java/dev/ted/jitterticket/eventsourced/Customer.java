package dev.ted.jitterticket.eventsourced;

public class Customer extends EventSourcedAggregate<CustomerEvent> {

    public static Customer register(String name, String email) {
        return new Customer(name, email);
    }

    public Customer(String name, String email) {
        enqueue(new CustomerRegistered(name, email));
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {

    }
}
