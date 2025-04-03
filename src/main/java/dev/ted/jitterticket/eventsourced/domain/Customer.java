package dev.ted.jitterticket.eventsourced.domain;

import java.util.List;
import java.util.StringJoiner;

public class Customer extends EventSourcedAggregate<CustomerEvent, CustomerId> {

    private String name;
    private String email;

    public static Customer register(CustomerId customerId, String name, String email) {
        return new Customer(customerId, name, email);
    }

    public static Customer reconstitute(List<CustomerEvent> customerEvents) {
        return new Customer(customerEvents);
    }

    private Customer(List<CustomerEvent> customerEvents) {
        customerEvents.forEach(this::apply);
    }

    private Customer(CustomerId customerId, String name, String email) {
        enqueue(new CustomerRegistered(customerId, name, email));
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {
        switch (customerEvent) {
            case CustomerRegistered(CustomerId customerId, String customerName, String email) -> {
                setId(customerId);
                this.name = customerName;
                this.email = email;
            }
        }
    }

    public String name() {
        return name;
    }

    public String email() {
        return email;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Customer.class.getSimpleName() + "[", "]")
                .add("id='" + getId() + "'")
                .add("name='" + name + "'")
                .add("email='" + email + "'")
                .toString();
    }
}
