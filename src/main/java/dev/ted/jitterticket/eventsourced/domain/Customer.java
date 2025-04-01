package dev.ted.jitterticket.eventsourced.domain;

import java.util.List;

public class Customer extends EventSourcedAggregate<CustomerEvent> {

    private String name;
    private String email;

    public static Customer register(String name, String email) {
        return new Customer(name, email);
    }

    public static Customer reconstitute(List<CustomerEvent> customerEvents) {
        return new Customer(customerEvents);
    }

    private Customer(List<CustomerEvent> customerEvents) {
        customerEvents.forEach(this::apply);
    }

    private Customer(String name, String email) {
        enqueue(new CustomerRegistered(name, email));
    }

    @Override
    protected void apply(CustomerEvent customerEvent) {
        switch (customerEvent) {
            case CustomerRegistered(String customerName, String email) -> {
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
}
