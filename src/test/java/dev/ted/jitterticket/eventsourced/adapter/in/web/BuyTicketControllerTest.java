package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.*;

class BuyTicketControllerTest {

    @Test
    void buyTicketsViewPutsConcertAndTicketOrderIntoModel() {
        BuyTicketController buyTicketController = new BuyTicketController();

        Model model = new ConcurrentModel();
        String concertIdString = "af05fc05-2de1-46d8-9568-01381029feb7";
        String viewName = buyTicketController.buyTicketsView(model, concertIdString);

        assertThat(viewName)
                .isEqualTo("buy-tickets");
        ConcertView concertView = (ConcertView) model.getAttribute("concert");
        assertThat(concertView)
                .isNotNull();
        TicketOrderForm ticketOrderForm = (TicketOrderForm) model.getAttribute("ticketOrderForm");
        assertThat(ticketOrderForm)
                .isNotNull();
    }
}