package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/event-viewer")
public class EventViewerController {

    private final ConcertProjector concertProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Autowired
    public EventViewerController(ConcertProjector concertProjector, EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertProjector = concertProjector;
        this.concertStore = concertStore;
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

    @GetMapping("/{concertId}")
    public String showConcertEvents(@PathVariable("concertId") String concertIdString,
                                    Model model) {
        ConcertId concertId = new ConcertId(java.util.UUID.fromString(concertIdString));
        List<ConcertEvent> concertEvents = concertStore
                .eventsForAggregate(concertId)
                .reversed();
        model.addAttribute("concertId", concertIdString);
        model.addAttribute("events", concertEvents);
        return "event-viewer/concert-events";
    }
}
