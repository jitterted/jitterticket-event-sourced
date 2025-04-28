package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummary;
import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ConcertsController {

    private final ConcertSummaryProjector concertSummaryProjector;

    @Autowired
    public ConcertsController(ConcertSummaryProjector concertSummaryProjector) {
        this.concertSummaryProjector = concertSummaryProjector;
    }

    @GetMapping("/concerts")
    public String ticketableConcerts(Model model) {
        List<ConcertView> concertViews =
                concertSummaryProjector.allConcertSummaries()
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
