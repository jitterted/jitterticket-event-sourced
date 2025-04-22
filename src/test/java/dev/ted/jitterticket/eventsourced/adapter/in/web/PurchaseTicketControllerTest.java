package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.application.PurchaseTicketsUseCase;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;

class PurchaseTicketControllerTest {

    @Test
    void purchaseTicketsViewPutsConcertAndTicketOrderIntoModel() {
        var concertStore = EventStore.forConcerts();
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(Concert.schedule(
                concertId,
                "Midnight Rebels",
                55,
                LocalDateTime.of(2025, 9, 15, 21, 0),
                LocalTime.of(20, 0),
                150,
                4));
        var customerStore = EventStore.forCustomers();
        PurchaseTicketController purchaseTicketController =
                new PurchaseTicketController(concertStore, new PurchaseTicketsUseCase(concertStore, customerStore));
        String concertIdString = concertId.id().toString();

        Model model = new ConcurrentModel();
        String viewName = purchaseTicketController.purchaseTicketsView(model, concertIdString);

        assertThat(viewName)
                .isEqualTo("purchase-tickets");
        ConcertView concertView = (ConcertView) model.getAttribute("concert");
        assertThat(concertView)
                .isEqualTo(new ConcertView(
                        concertIdString,
                        "Midnight Rebels",
                        "$55",
                        "September 15, 2025", "9:00\u202FPM"
                ));
        TicketOrderForm ticketOrderForm = (TicketOrderForm) model.getAttribute("ticketOrderForm");
        assertThat(ticketOrderForm.customerId())
                .isNotBlank();
        assertThat(ticketOrderForm.quantity())
                .isEqualTo(2);
    }

    @Test
    void placeTicketOrderRedirectsToOrderConfirmationPage() {
        var concertStore = EventStore.forConcerts();
        var customerStore = EventStore.forCustomers();
        TicketOrderId ticketOrderId = TicketOrderId.createRandom();
        PurchaseTicketsUseCase purchaseTicketsUseCase =
                PurchaseTicketsUseCase.createForTest(concertStore,
                                                     customerStore,
                                                     ticketOrderId);
        PurchaseTicketController purchaseTicketController =
                new PurchaseTicketController(concertStore, purchaseTicketsUseCase);
        ConcertId concertId = ConcertId.createRandom();
        int initialCapacity = 100;
        concertStore.save(Concert.schedule(concertId, "Pulse Wave", 40, LocalDateTime.of(2025, 11, 8, 22, 30), LocalTime.of(21, 0), initialCapacity, 4));
        CustomerId customerId = CustomerId.createRandom();
        customerStore.save(Customer.register(customerId, "Customer Name", "customer@example.com"));

        int numberOfTicketsToPurchase = 4;
        String redirectString = purchaseTicketController
                .purchaseTickets(concertId.id().toString(),
                                 new TicketOrderForm(
                                         customerId.id().toString(),
                                         numberOfTicketsToPurchase));

        String ticketOrderUuidString = ticketOrderId.id().toString();
        String customerUuidString = customerId.id().toString();
        assertThat(redirectString)
                .isEqualTo("redirect:/customers/" + customerUuidString
                           + "/confirmations/" + ticketOrderUuidString);

        Concert concert = concertStore.findById(concertId).orElseThrow();
        assertThat(concert.availableTicketCount())
                .isEqualTo(initialCapacity - numberOfTicketsToPurchase);
    }
}