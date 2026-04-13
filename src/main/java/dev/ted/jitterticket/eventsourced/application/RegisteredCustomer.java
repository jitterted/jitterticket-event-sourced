package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;

public record RegisteredCustomer(CustomerId customerId, String name) {}
