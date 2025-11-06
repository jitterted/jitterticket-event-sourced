package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(PurchaseTicketsController.class)
class PurchaseTicketsControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Autowired
    EventStore<CustomerId, CustomerEvent, Customer> customerStore;

    @Test
    void getToPurchaseTicketViewEndpointReturns200Ok() {
        ConcertId concertId = ConcertFactory.Store.saveScheduledConcertIn(concertStore);

        mvc.get()
           .uri("/concerts/" + concertId.id().toString())
           .assertThat()
           .hasStatus2xxSuccessful();
    }

    @Test
    void postToPurchaseTicketEndpointRedirects() {
        Customer customer = CustomerFactory.newlyRegistered();
        customerStore.save(customer);
        ConcertId concertId = ConcertFactory.Store.saveScheduledConcertIn(concertStore);
        mvc.post()
           .formField("customerId", customer.getId().id().toString())
           .formField("quantity", "2")
           .uri("/concerts/" + concertId.id().toString())
           .assertThat()
           .hasStatus3xxRedirection();
    }

}