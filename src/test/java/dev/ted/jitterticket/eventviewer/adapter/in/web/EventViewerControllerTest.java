package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.TixConfiguration;
import dev.ted.jitterticket.eventsourced.application.ConcertSummaryProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("unchecked")
class EventViewerControllerTest {

    @Test
    void listProjectionChoicesShowsAvailableProjections() {
        var concertStore = EventStore.forConcerts();
        var customerStore = EventStore.forCustomers();
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);
        ProjectionChoice concertProjectionChoice = createConcertProjectionChoice(TixConfiguration.functionForUuidToConcertEvents(concertStore), TixConfiguration.functionForConcertEventsToStrings());
        EventViewerController controller =
                new EventViewerController(concertSummaryProjector,
                                          concertProjectionChoice);
        ConcurrentModel model = new ConcurrentModel();

        String viewName = controller.listProjectionChoices(model);

        assertThat(viewName)
                .isEqualTo("event-viewer/projection-choices");
        assertThat(model)
                .containsEntry("projections",
                               List.of(concertProjectionChoice,
                                       new ProjectionChoice("Concert Summaries", "/event-viewer/concert-summaries", null, null),
                                       new ProjectionChoice("Customers", "/event-viewer/customers", null, null)
                               )
                );
    }

    private static ProjectionChoice createConcertProjectionChoice(Function<UUID, List<? extends Event>> uuidToAllEventsForConcert, Function<List<? extends Event>, List<String>> eventsToStrings) {
        return new ProjectionChoice("Concerts", "/event-viewer/concerts", uuidToAllEventsForConcert, eventsToStrings);
    }

    @Test
    void listConcertsReturnsCorrectViewName() {
        var concertStore = EventStore.forConcerts();
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);
        EventViewerController controller = new
                EventViewerController(concertSummaryProjector,
                                      createConcertProjectionChoice(TixConfiguration.functionForUuidToConcertEvents(concertStore),
                                                                    TixConfiguration.functionForConcertEventsToStrings()));

        ConcurrentModel model = new ConcurrentModel();

        String viewName = controller.listConcerts(model);

        assertThat(viewName)
                .isEqualTo("event-viewer/concert-aggregates");
    }

    @Test
    void listConcertsAddsCorrectAttributesToModel() {
        var concertStore = EventStore.forConcerts();
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);

        ConcertId concertId = ConcertId.createRandom();
        String artist = "Test Artist";
        LocalDateTime showDateTime = LocalDateTime.of(2025, 7, 26, 20, 0);
        LocalTime doorsTime = LocalTime.of(19, 0);

        concertStore.save(ConcertFactory.scheduleConcertWith(concertId,
                                                             artist,
                                                             100,
                                                             showDateTime,
                                                             doorsTime));

        EventViewerController controller = new EventViewerController(concertSummaryProjector,
                                                                     createConcertProjectionChoice(TixConfiguration.functionForUuidToConcertEvents(concertStore), TixConfiguration.functionForConcertEventsToStrings()));
        Model model = new ConcurrentModel();

        controller.listConcerts(model);

        List<ConcertListView> concertList = (List<ConcertListView>) model.getAttribute("concerts");
        assertThat(concertList)
                .hasSize(1)
                .extracting(ConcertListView::concertId, ConcertListView::artist)
                .containsExactly(tuple(concertId.id().toString(), artist));
    }

    @Test
    void showConcertEventsReturnsCorrectViewNameAndModelContentsForMostRecentEvent() {
        Fixture fixture = createAndSaveConcertWithThreeEvents();

        ConcurrentModel model = new ConcurrentModel();
        int selectedEventSequence = fixture.concertEvents.getLast().eventSequence();
        String viewName = fixture.controller().showConcertEvents(
                fixture.concertIdAsString, selectedEventSequence, model);

        assertThat(viewName)
                .isEqualTo("event-viewer/concert-events");

        assertThat(model)
                .containsEntry("concertId", fixture.concertIdAsString);

        List<EventView> events = (List<EventView>) model.getAttribute("events");
        assertThat(events)
                .hasOnlyElementsOfType(EventView.class)
                .hasSize(fixture.concertEvents.size());

        assertThat(model)
                .extracting("projectedState", InstanceOfAssertFactories.list(String.class))
                .containsExactly("Artist: " + fixture.concert().artist(),
                                 "Show Time: " + fixture.concert().showDateTime(),
                                 "Doors Time: " + fixture.concert().doorsTime(),
                                 "Tickets Remaining: " + fixture.concert().availableTicketCount()
                );
    }

    @Test
    void defaultSelectedEventForShowConcertIsMostRecentEvent() {
        Fixture fixture = createAndSaveConcertWithThreeEvents();

        ConcurrentModel model = new ConcurrentModel();
        int selectedEventAsDefault = -1;
        fixture.controller().showConcertEvents(fixture.concertIdAsString, selectedEventAsDefault, model);

        Integer defaultSelectedEvent = fixture.concertEvents.getLast().eventSequence();
        assertThat(model)
                .containsEntry("selectedEvent", defaultSelectedEvent);
    }

    @ParameterizedTest
    @ValueSource(ints = {-999, 3, 999})
    void outOfRangeSelectedEventsAreClampedToMostRecentEvent(int badSelectedEvent) {
        Fixture fixture = createAndSaveConcertWithThreeEvents();

        ConcurrentModel model = new ConcurrentModel();
        fixture.controller().showConcertEvents(fixture.concertIdAsString,
                                               badSelectedEvent, model);

        Integer mostRecentEventSequence = fixture.concertEvents.getLast().eventSequence();
        assertThat(model)
                .containsEntry("selectedEvent", mostRecentEventSequence);
    }

    @Test
    void selectedEventForShowConcertShowsStateAsOfSelectedEventWithAllEventsDisplayed() {
        Fixture fixture = createAndSaveConcertWithThreeEvents();

        ConcurrentModel model = new ConcurrentModel();
        fixture.controller().showConcertEvents(fixture.concertIdAsString,
                                               1,
                                               model);

        assertThat(model)
                .containsEntry("selectedEvent", 1)
                .extracting("events", InstanceOfAssertFactories.list(ConcertEvent.class))
                .as("All events for the concert should be displayed, regardless of selected event")
                .hasSize(fixture.concertEvents.size());
    }

    private static Fixture createAndSaveConcertWithThreeEvents() {
        var concertStore = EventStore.forConcerts();
        ConcertSummaryProjector concertSummaryProjector = new ConcertSummaryProjector(concertStore);

        ConcertId concertId = ConcertId.createRandom();
        Concert concert = ConcertFactory.scheduleConcertWith(
                concertId,
                "Test Artist",
                100,
                LocalDateTime.of(2025, 7, 26, 20, 0),
                LocalTime.of(19, 0));
        concert.rescheduleTo(concert.showDateTime().plusMonths(2), concert.doorsTime());
        concert.sellTicketsTo(CustomerId.createRandom(), 4);
        concertStore.save(concert);

        EventViewerController controller = new EventViewerController(concertSummaryProjector,
                                                                     createConcertProjectionChoice(TixConfiguration.functionForUuidToConcertEvents(concertStore), TixConfiguration.functionForConcertEventsToStrings()));
        return new Fixture(concertId.id().toString(), concert, controller, concertStore.eventsForAggregate(concertId));
    }

    private record Fixture(String concertIdAsString, Concert concert, EventViewerController controller,
                           List<ConcertEvent> concertEvents) {}

}
