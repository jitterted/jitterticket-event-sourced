package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.TixConfiguration;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Tag("mvc")
@Tag("spring")
@WebMvcTest(PurchaseTicketController.class)
@Import(TixConfiguration.class)
class PurchaseTicketControllerMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Autowired
    EventStore<CustomerId, CustomerEvent, Customer> customerStore;

    @Test
    void getToPurchaseTicketViewEndpointReturns200Ok() {
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(Concert.schedule(
                concertId,
                "Blue Note Quartet",
                35,
                LocalDateTime.of(2025, 8, 22, 19, 30),
                LocalTime.of(18, 30),
                75,
                2));

        mvc.get()
           .uri("/concerts/" + concertId.id().toString())
           .assertThat()
           .hasStatus2xxSuccessful();
    }

    @Test
    void postToPurchaseTicketEndpointRedirects() {
        Customer customer = CustomerFactory.newlyRegistered();
        customerStore.save(customer);
        ConcertId concertId = ConcertId.createRandom();
        mvc.post()
           .formField("customerId", customer.getId().id().toString())
           .formField("quantity", "2")
           .uri("/concerts/" + concertId.id().toString())
           .assertThat()
           .hasStatus3xxRedirection();
    }
}