package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CustomersController {

    @GetMapping("/customers/{customerId}/confirmations/{ticketOrderId}")
    public String viewPurchaseConfirmation(Model model) {

        return "purchase-confirmation";
    }

}
