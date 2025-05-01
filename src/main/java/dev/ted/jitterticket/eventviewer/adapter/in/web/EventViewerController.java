package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/event-viewer")
/*
 * Look into using https://docs.spring.io/spring-framework/reference/web/webmvc-functional.html
 * as an alternative approach instead of this controller using the projection choice's methods
 * have the project choice completely handle the request
 */
public class EventViewerController {

    protected final Map<String, ProjectionChoice> projectionChoices;

    @Autowired
    public EventViewerController(ProjectionChoice projectionChoice) {
        this.projectionChoices = Map.of("concerts", projectionChoice);
    }

    @GetMapping
    public String listProjectionChoices(Model model) {
        model.addAttribute("projections",
                           projectionChoices.values());
        return "event-viewer/projection-choices";
    }

    @GetMapping("/{aggregateName}")
    public String listAggregates(@PathVariable("aggregateName") String aggregateName,
                                 Model model) {
        ProjectionChoice choice = projectionChoices.get(aggregateName);
        model.addAttribute("aggregateName", choice.aggregateName());
        model.addAttribute("aggregates", choice.aggregateSummaryViews());
        return "event-viewer/list-aggregates";
    }

    @GetMapping("/{aggregateName}/{uuidString}")
    public String showConcertEvents(@PathVariable("aggregateName") String aggregateName,
                                    @PathVariable("uuidString") String uuidString,
                                    @RequestParam(value = "selectedEvent", required = false, defaultValue = "-1") int selectedEvent,
                                    Model model) {
        model.addAttribute("uuid", uuidString);
        UUID uuid = UUID.fromString(uuidString);
        ProjectionChoice choice = projectionChoices.get(aggregateName);
        List<? extends Event> allEvents = choice.eventsFor(uuid);
        if (selectedEvent < 0 || selectedEvent > allEvents.getLast().eventSequence()) {
            selectedEvent = allEvents.getLast().eventSequence();
        }
        model.addAttribute("selectedEvent", selectedEvent);
        model.addAttribute("events", eventViewsOf(allEvents));
        model.addAttribute("aggregateName", choice.aggregateName());

        List<? extends Event> selectedConcertEvents = selectEvents(selectedEvent, allEvents);
        List<String> aggregateProperties = choice.propertiesOfProjectionFrom(selectedConcertEvents);
        model.addAttribute("projectedState", aggregateProperties);
        // generalize the Thymeleaf template to work with any projection type
        return "event-viewer/concert-events";
    }

    private static List<? extends Event> selectEvents(int selectedEvent, List<? extends Event> allEvents) {
        int eventIndex = allEvents.size() - 1;
        for (int i = 0; i < allEvents.size(); i++) {
            if (allEvents.get(i).eventSequence() == selectedEvent) {
                eventIndex = i;
                break;
            }
        }
        return allEvents.subList(0, eventIndex + 1);
    }

    private static List<EventView> eventViewsOf(List<? extends Event> allEvents) {
        return allEvents.reversed()
                        .stream()
                        .map(EventView::of)
                        .toList();
    }

}
