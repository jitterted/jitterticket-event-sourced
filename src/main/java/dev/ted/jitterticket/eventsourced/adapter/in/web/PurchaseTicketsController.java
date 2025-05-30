package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.PurchaseTicketsUseCase;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
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

import java.util.Optional;
import java.util.UUID;

@Controller
public class PurchaseTicketsController {

    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;
    private final PurchaseTicketsUseCase purchaseTicketsUseCase;

    @Autowired
    public PurchaseTicketsController(EventStore<ConcertId, ConcertEvent, Concert> concertStore,
                                     PurchaseTicketsUseCase purchaseTicketsUseCase) {
        this.concertStore = concertStore;
        this.purchaseTicketsUseCase = purchaseTicketsUseCase;
    }

    @GetMapping("/concerts/{concertId}")
    public String purchaseTicketsView(Model model,
                                      @PathVariable("concertId") String concertId) {
        model.addAttribute("concert", concertViewFor(concertId));
        String customerUuidString = "68f5b2c2-d70d-4992-ad78-c94809ae9a6a";
        model.addAttribute("ticketOrderForm", new TicketOrderForm(
                customerUuidString, 2));
        return "purchase-tickets";
    }

    @PostMapping("/concerts/{concertId}")
    public String purchaseTickets(@PathVariable("concertId") String concertId,
                                  TicketOrderForm ticketOrderForm) {
        Optional<TicketOrderId> ticketOrderId = purchaseTicketsUseCase
                .purchaseTickets(
                        new ConcertId(UUID.fromString(concertId)),
                        new CustomerId(UUID.fromString(ticketOrderForm.customerId())),
                        ticketOrderForm.quantity());
        String ticketOrderUuid = ticketOrderId.map(TicketOrderId::id)
                                              .map(UUID::toString)
                                              .orElseThrow();
        return "redirect:/customers/" + ticketOrderForm.customerId() +
               "/confirmations/" + ticketOrderUuid;
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
