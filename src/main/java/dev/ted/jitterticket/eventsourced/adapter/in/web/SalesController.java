package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SalesController {

    @GetMapping("/concert-sales")
    public String viewConcertSalesSummary(Model model) {
        model.addAttribute("salesSummary", "ahoy hoy");
        return "concert-sales-view";
    }
}
