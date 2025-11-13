package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class SalesController {

    @GetMapping("/concert-sales")
    public String getSalesView() {
        return "concert-sales-view";
    }
}
