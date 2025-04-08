package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@Controller
public class BuyTicketController {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Autowired
    public BuyTicketController(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
    }

    @GetMapping("/concerts/{concertId}")
    public String buyTicketsView(Model model,
                                 @PathVariable("concertId") String concertId) {
        model.addAttribute("concert", concertViewFor(concertId));
        model.addAttribute("ticketOrderForm", new TicketOrderForm(
                UUID.randomUUID().toString(), 2));
        return "buy-tickets";
    }

    @PostMapping("/concerts/{concertId}")
    public String buyTickets(@PathVariable("concertId") String concertId) {
        return "redirect:/";
    }

    private ConcertView concertViewFor(String concertId) {
        return concertStore.findById(
                new ConcertId(UUID.fromString(concertId)))
                           .map(ConcertView::from)
                           .orElseThrow(() -> new RuntimeException("Could not find concert with id: " + concertId));
    }

}

record TicketOrderForm(String customerId, int quantity) {}
