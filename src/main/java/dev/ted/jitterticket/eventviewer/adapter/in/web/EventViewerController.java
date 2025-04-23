package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/event-viewer")
public class EventViewerController {

    private final ConcertProjector concertProjector;

    @Autowired
    public EventViewerController(ConcertProjector concertProjector) {
        this.concertProjector = concertProjector;
    }

    @GetMapping
    public String listConcerts(Model model) {
        List<ConcertListView> concertListViews =
                concertProjector.allConcertSummaries()
                        .map(ConcertListView::from)
                        .toList();
        model.addAttribute("concerts", concertListViews);
        return "event-viewer/concert-aggregates";
    }
}