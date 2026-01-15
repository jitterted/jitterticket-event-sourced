package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.AvailableConcert;
import dev.ted.jitterticket.eventsourced.application.AvailableConcerts;
import dev.ted.jitterticket.eventsourced.application.AvailableConcertsDelta;
import dev.ted.jitterticket.eventsourced.application.ProjectionCoordinator;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ConcertsController {

    private final ProjectionCoordinator<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> availableConcertsProjection;

    @Autowired
    public ConcertsController(ProjectionCoordinator<ConcertEvent, AvailableConcerts, AvailableConcertsDelta> availableConcertsProjection) {
        this.availableConcertsProjection = availableConcertsProjection;
    }

    @GetMapping("/concerts")
    public String ticketableConcerts(Model model) {
        List<ConcertView> concertViews =
                availableConcertsProjection.projection()
                                           .availableConcerts()
                                           .stream()
                                           .map(this::toConcertView)
                                           .toList();
        model.addAttribute("concerts", concertViews);
        return "concerts";
    }

    private ConcertView toConcertView(AvailableConcert availableConcert) {
        return ConcertView.create(availableConcert.concertId(),
                                  availableConcert.artist(),
                                  availableConcert.showDateTime(),
                                  availableConcert.ticketPrice());
    }

}
