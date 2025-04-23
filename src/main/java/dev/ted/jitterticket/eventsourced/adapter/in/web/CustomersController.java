package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@Controller
public class CustomersController {

    private EventStore<CustomerId, CustomerEvent, Customer> customerStore;
    private EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    public CustomersController(EventStore<CustomerId, CustomerEvent, Customer> customerStore, EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.customerStore = customerStore;
        this.concertStore = concertStore;
    }

    @GetMapping("/customers/{customerId}/confirmations/{ticketOrderId}")
    public String viewPurchaseConfirmation(Model model,
                                           @PathVariable("customerId") String customerId,
                                           @PathVariable("ticketOrderId") String ticketOrderId) {
        Customer customer = customerStore.findById(new CustomerId(UUID.fromString(customerId)))
                                         .orElseThrow(() -> new RuntimeException("Customer not found for ID: " + customerId));
        Customer.TicketOrder ticketOrder = customer.ticketFor(new TicketOrderId(UUID.fromString(ticketOrderId)));
        Concert concert = concertStore.findById(ticketOrder.concertId())
                                              .orElseThrow(() -> new RuntimeException("Concert not found for ID: " + ticketOrder.concertId()));

        model.addAttribute("numberOfTickets", ticketOrder.quantity());
        model.addAttribute("artist", concert.artist());
        model.addAttribute("showDateTime", concert.showDateTime());
        return "purchase-confirmation";
    }

}
