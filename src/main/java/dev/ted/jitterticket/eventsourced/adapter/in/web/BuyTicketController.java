package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class BuyTicketController {

    @GetMapping("/concerts/{concertId}")
    public String buyTicketsView(Model model,
                                 @PathVariable("concertId") String concertId) {
        model.addAttribute("concert", new ConcertView(
                concertId, "artist", "$99", "July 9, 2025", "8:00PM"
        ));
        model.addAttribute("ticketOrderForm", new TicketOrderForm(UUID.randomUUID().toString(), 2));
        return "buy-tickets";
    }

}

record TicketOrderForm(String customerId, int quantity) {}
