package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PurchaseTicketControllerTest {

    @Test
    void buyTicketsViewPutsConcertAndTicketOrderIntoModel() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        ConcertId concertId = ConcertId.createRandom();
        concertStore.save(Concert.schedule(
                concertId,
                "Midnight Rebels",
                55,
                LocalDateTime.of(2025, 9, 15, 21, 0),
                LocalTime.of(20, 0),
                150,
                4));
        PurchaseTicketController purchaseTicketController = new PurchaseTicketController(concertStore);
        String concertIdString = concertId.id().toString();

        Model model = new ConcurrentModel();
        String viewName = purchaseTicketController.buyTicketsView(model, concertIdString);

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
    @Disabled("dev.ted.jitterticket.eventsourced.adapter.in.web.PurchaseTicketControllerTest 4/21/25 14:20 — until use case fully working")
    void placeTicketOrderRedirectsToOrderConfirmationPage() {
        EventStore<ConcertId, ConcertEvent, Concert> concertStore = EventStore.forConcerts();
        PurchaseTicketController purchaseTicketController = new PurchaseTicketController(concertStore);
        ConcertId concertId = ConcertId.createRandom();
        int initialCapacity = 100;
        concertStore.save(Concert.schedule(concertId, "Pulse Wave", 40, LocalDateTime.of(2025, 11, 8, 22, 30), LocalTime.of(21, 0), initialCapacity, 4));
        String customerUuid = UUID.randomUUID().toString();

        int numberOfTicketsToBuy = 4;
        String redirectString = purchaseTicketController
                .buyTickets(concertId.id().toString(),
                            new TicketOrderForm(customerUuid,
                                                numberOfTicketsToBuy));

        String ticketOrderUuid = "af05fc05-2de1-46d8-9568-01381029feb7";
        assertThat(redirectString)
                .isEqualTo("redirect:/confirmations/" + ticketOrderUuid);

        Concert concert = concertStore.findById(concertId).orElseThrow();
        assertThat(concert.availableTicketCount())
                .isEqualTo(initialCapacity - numberOfTicketsToBuy);
    }
}