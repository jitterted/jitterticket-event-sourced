package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ConcertsController {

    @GetMapping("/concerts")
    public String ticketableConcerts(Model model) {
        return "concerts";
    }

}
