package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
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
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/event-viewer")
public class EventViewerController {

    private final ConcertSummaryProjector concertSummaryProjector;
    private final EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Autowired
    public EventViewerController(ConcertSummaryProjector concertSummaryProjector, EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        this.concertSummaryProjector = concertSummaryProjector;
        this.concertStore = concertStore;
    }

    @GetMapping
    public String listProjectionChoices(Model model) {
        model.addAttribute("projections", List.of(new EventViewerController.ProjectionChoice("Concerts", "/event-viewer/concerts"),
                                                  new EventViewerController.ProjectionChoice("Concert Summaries", "/event-viewer/concert-summaries"),
                                                  new EventViewerController.ProjectionChoice("Customers", "/event-viewer/customers")));
        return "event-viewer/projection-choices";
    }

    record ProjectionChoice(String description, String urlPath) {}

    @GetMapping("/concerts")
    public String listConcerts(Model model) {
        List<ConcertListView> concertListViews =
                concertSummaryProjector.allConcertSummaries()
                                       .map(ConcertListView::of)
                                       .toList();
        model.addAttribute("concerts", concertListViews);
        return "event-viewer/concert-aggregates";
    }

    @GetMapping("/concerts/{concertId}")
    public String showConcertEvents(@PathVariable("concertId") String concertIdString,
                                    @RequestParam(value = "selectedEvent", required = false, defaultValue = "-1") int selectedEvent,
                                    Model model) {
        model.addAttribute("concertId", concertIdString);
        ConcertId concertId = new ConcertId(UUID.fromString(concertIdString));
        List<ConcertEvent> allConcertEvents = concertStore.eventsForAggregate(concertId);
        if (selectedEvent < 0 || selectedEvent > allConcertEvents.getLast().eventSequence()) {
            selectedEvent = allConcertEvents.getLast().eventSequence();
        }
        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("events", eventViewsOf(allConcertEvents));

        Concert concert = reconstituteThroughSelectedEventSequence(selectedEvent, allConcertEvents);
        model.addAttribute("projectedState",
                           List.of(
                                   "Artist: " + concert.artist(),
                                   "Show Time: " + concert.showDateTime(),
                                   "Doors Time: " + concert.doorsTime(),
                                   "Tickets Remaining: " + concert.availableTicketCount()
                           ));
        return "event-viewer/concert-events";
    }

    private static List<EventView> eventViewsOf(List<ConcertEvent> allConcertEvents) {
        return allConcertEvents.reversed().stream()
                               .map(EventView::of)
                               .toList();
    }

    private static Concert reconstituteThroughSelectedEventSequence(int selectedEvent, List<ConcertEvent> allConcertEvents) {
        int eventIndex = allConcertEvents.size() - 1;
        for (int i = 0; i < allConcertEvents.size(); i++) {
            if (allConcertEvents.get(i).eventSequence() == selectedEvent) {
                eventIndex = i;
                break;
            }
        }
        List<ConcertEvent> selectedConcertEvents = allConcertEvents.subList(0, eventIndex + 1);
        return Concert.reconstitute(selectedConcertEvents);
    }

}
