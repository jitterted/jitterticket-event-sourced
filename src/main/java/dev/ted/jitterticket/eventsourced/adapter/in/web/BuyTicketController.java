package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class BuyTicketController {

    @GetMapping("/concerts/{concertId}")
    public String buyTicketsView(Model model,
                                 @PathVariable("concertId") String concertId) {
        return "buy-tickets";
    }

}
