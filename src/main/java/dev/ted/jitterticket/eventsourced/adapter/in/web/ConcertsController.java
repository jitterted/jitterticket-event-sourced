package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.ConcertSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ConcertsController {

    private final ConcertProjector concertProjector;

    @Autowired
    public ConcertsController(ConcertProjector concertProjector) {
        this.concertProjector = concertProjector;
    }

    @GetMapping("/concerts")
    public String ticketableConcerts(Model model) {
        List<ConcertView> concertViews =
                concertProjector.allConcertTicketViews()
                                .map(this::convertToConcertView)
                                .toList();
        model.addAttribute("concerts", concertViews);
        return "concerts";
    }

    private ConcertView convertToConcertView(ConcertSummary concertSummary) {
        return ConcertView.create(concertSummary.concertId(),
                                  concertSummary.artist(),
                                  concertSummary.showDateTime(),
                                  concertSummary.ticketPrice());
    }

}
