package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import dev.ted.jitterticket.eventsourced.domain.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Controller
@RequestMapping("/event-viewer")
public class EventViewerController {

    private final ConcertSummaryProjector concertSummaryProjector;
    private final Function<List<? extends Event>, List<String>> eventsToStrings;
    private final Function<UUID, List<? extends Event>> uuidToAllEventsForConcert;

    @Autowired
    public EventViewerController(ConcertSummaryProjector concertSummaryProjector,
                                 ProjectionChoice concertProjectionChoice) {
        this.concertSummaryProjector = concertSummaryProjector;
        this.uuidToAllEventsForConcert = concertProjectionChoice.uuidToAllEvents();
        this.eventsToStrings = concertProjectionChoice.eventsToStrings();
    }

    @GetMapping
    public String listProjectionChoices(Model model) {
        model.addAttribute("projections",
                           List.of(new ProjectionChoice("Concerts", "/event-viewer/concerts", uuidToAllEventsForConcert, eventsToStrings),
                                   new ProjectionChoice("Concert Summaries", "/event-viewer/concert-summaries", null, null),
                                   new ProjectionChoice("Customers", "/event-viewer/customers", null, null)));
        return "event-viewer/projection-choices";
    }

    @GetMapping("/concerts")
    public String listAggregates(Model model) {
        List<ConcertListView> concertListViews =
                concertSummaryProjector.allConcertSummaries()
                                       .map(ConcertListView::of)
                                       .toList();
        model.addAttribute("concerts", concertListViews);
        return "event-viewer/concert-aggregates";
    }

    @GetMapping("/concerts/{concertId}")
    // @GetMapping("/{aggType}/{concertId}")
    // get from the ProjectionChoiceMap(aggType) -> ProjectionChoice,
    // where we can use its functions instead of using the fields passed in via the constructor
    public String showConcertEvents(@PathVariable("concertId") String concertIdString,
                                    @RequestParam(value = "selectedEvent", required = false, defaultValue = "-1") int selectedEvent,
                                    Model model) {
        model.addAttribute("concertId", concertIdString);
        UUID uuid = UUID.fromString(concertIdString);
        List<? extends Event> allEvents = uuidToAllEventsForConcert.apply(uuid);
        if (selectedEvent < 0 || selectedEvent > allEvents.getLast().eventSequence()) {
            selectedEvent = allEvents.getLast().eventSequence();
        }
        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("events", eventViewsOf(allEvents));

        List<String> aggregateProperties = projectionPropertiesFrom(selectedEvent, allEvents, eventsToStrings);
        model.addAttribute("projectedState", aggregateProperties);
        // generalize the Thymeleaf template to work with any projection type
        return "event-viewer/concert-events";
    }

    private static List<String> projectionPropertiesFrom(int selectedEvent,
                                                         List<? extends Event> allEvents,
                                                         Function<List<? extends Event>, List<String>> fnEventsToStrings) {
        int eventIndex = allEvents.size() - 1;
        for (int i = 0; i < allEvents.size(); i++) {
            if (allEvents.get(i).eventSequence() == selectedEvent) {
                eventIndex = i;
                break;
            }
        }
        List<? extends Event> selectedConcertEvents = allEvents.subList(0, eventIndex + 1);
        return fnEventsToStrings.apply(selectedConcertEvents);
    }

    private static List<EventView> eventViewsOf(List<? extends Event> allEvents) {
        return allEvents.reversed()
                        .stream()
                        .map(EventView::of)
                        .toList();
    }

}
