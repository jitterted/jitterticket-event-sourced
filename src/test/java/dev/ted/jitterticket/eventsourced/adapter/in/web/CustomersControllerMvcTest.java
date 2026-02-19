package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(CustomersController.class)
class CustomersControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Autowired
    EventStore<CustomerId, CustomerEvent, Customer> customerStore;

    @Test
    void getToCustomerTicketPurchaseConfirmationReturns200Ok() {
        Concert concert = ConcertFactory.createConcert();
        concertStore.save(concert);
        Customer customer = CustomerFactory.newlyRegistered();
        TicketOrderId ticketOrderId = TicketOrderId.createRandom();
        customer.purchaseTickets(concert, ticketOrderId, 1);
        customerStore.save(customer);
        mvc.get()
           .uri("/customers/" + customer.getId().id() + "/confirmations/" + ticketOrderId.id())
           .assertThat()
           .hasStatus2xxSuccessful();
    }
}