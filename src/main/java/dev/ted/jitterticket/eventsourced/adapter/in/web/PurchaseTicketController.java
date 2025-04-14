package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.application.PurchaseTicketsUseCase;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

@Controller
public class PurchaseTicketController {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;
    private final PurchaseTicketsUseCase purchaseTicketsUseCase;

    @Autowired
    public PurchaseTicketController(EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertStore = concertStore;
        purchaseTicketsUseCase = new PurchaseTicketsUseCase(concertStore);
    }

    @GetMapping("/concerts/{concertId}")
    public String buyTicketsView(Model model,
                                 @PathVariable("concertId") String concertId) {
        model.addAttribute("concert", concertViewFor(concertId));
        model.addAttribute("ticketOrderForm", new TicketOrderForm(
                UUID.randomUUID().toString(), 2));
        return "purchase-tickets";
    }

    @PostMapping("/concerts/{concertId}")
    public String buyTickets(@PathVariable("concertId") String concertId,
                             TicketOrderForm ticketOrderForm) {
        purchaseTicketsUseCase.purchaseTickets(
                new ConcertId(UUID.fromString(concertId)),
                new CustomerId(UUID.fromString(ticketOrderForm.customerId())),
                ticketOrderForm.quantity());
        String ticketOrderUuid = "af05fc05-2de1-46d8-9568-01381029feb7";
        return "redirect:/confirmations/" + ticketOrderUuid;
    }

    // @GetMapping("/confirmations/{ticketOrderId}?customerId={customerId}

    private ConcertView concertViewFor(String concertId) {
        return concertStore.findById(
                                   new ConcertId(UUID.fromString(concertId)))
                           .map(ConcertView::from)
                           .orElseThrow(() -> new RuntimeException("Could not find concert with id: " + concertId));
    }

}

record TicketOrderForm(String customerId, int quantity) {}
