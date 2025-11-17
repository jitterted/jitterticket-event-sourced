package dev.ted.jitterticket.eventsourced.domain.customer;

import java.util.Objects;
import java.util.StringJoiner;

public final class CustomerRegistered extends CustomerEvent {
    private final String customerName;
    private final String email;

    public static CustomerRegistered createNew(CustomerId customerId,
                                               Integer eventSequence,
                                               String customerName,
                                               String email) {
        return new CustomerRegistered(customerId, eventSequence, customerName, email);
    }

//    @ReconstituteOnly // do not call unless from EventStore or a test
    public static CustomerRegistered reconstitute(CustomerId customerId,
                                                  Integer eventSequence,
                                                  Long globalEventSequence,
                                                  String customerName,
                                                  String email) {
        return new CustomerRegistered(customerId, eventSequence, globalEventSequence, customerName, email);
    }

    public CustomerRegistered(CustomerId customerId,
                              Integer eventSequence,
                              Long globalEventSequence,
                              String customerName,
                              String email) {
        super(customerId, eventSequence, globalEventSequence);
        this.customerName = customerName;
        this.email = email;
    }

    private CustomerRegistered(CustomerId customerId,
                               Integer eventSequence,
                               String customerName,
                               String email) {
        super(customerId, eventSequence);
        this.customerName = customerName;
        this.email = email;
    }

    public String customerName() {
        return customerName;
    }

    public String email() {
        return email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CustomerRegistered that = (CustomerRegistered) o;
        return Objects.equals(customerId(), that.customerId()) &&
               Objects.equals(eventSequence(), that.eventSequence()) &&
               Objects.equals(customerName, that.customerName) &&
               Objects.equals(email, that.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId(), eventSequence(), customerName, email);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CustomerRegistered.class.getSimpleName() + "[", "]")
                .add("customerId='" + customerId() + "'")
                .add("eventSequence=" + eventSequence())
                .add("customerName='" + customerName + "'")
                .add("email='" + email + "'")
                .toString();
    }
}
