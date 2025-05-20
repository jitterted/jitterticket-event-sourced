package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CustomersControllerTest {

    @Test
    void viewIncludesPurchaseConfirmationDetails() {
        var concertStore = InMemoryEventStore.forConcerts();
        String artist = "Character Set";
        LocalDateTime showDateTime = LocalDateTime.now();
        Concert concert = ConcertFactory.scheduleConcertWith(ConcertId.createRandom(),
                                                             artist,
                                                             42,
                                                             showDateTime,
                                                             showDateTime.minusHours(1).toLocalTime());
        concertStore.save(concert);
        var customerStore = InMemoryEventStore.forCustomers();
        Customer customer = CustomerFactory.newlyRegistered();
        customerStore.save(customer);
        int ticketQuantity = 3;
        TicketOrderId ticketOrderId = TicketOrderId.createRandom();
        customer.purchaseTickets(concert, ticketOrderId, ticketQuantity);
        customerStore.save(customer);
        CustomersController customersController = new CustomersController(customerStore, concertStore);

        ConcurrentModel model = new ConcurrentModel();
        String viewName = customersController.viewPurchaseConfirmation(model,
                                                                       customer.getId().id().toString(),
                                                                       ticketOrderId.id().toString());

        assertThat(viewName)
                .isEqualTo("purchase-confirmation");
        assertThat(model)
                .containsAllEntriesOf(
                        Map.of("numberOfTickets", ticketQuantity,
                               "artist", artist,
                               "showDateTime", showDateTime));
    }
}