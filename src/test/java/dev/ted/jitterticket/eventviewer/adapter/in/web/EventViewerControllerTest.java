package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("unchecked")
class EventViewerControllerTest {

    @Test
    void listConcertsReturnsCorrectViewName() {
        var concertStore = EventStore.forConcerts();
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        EventViewerController controller = new EventViewerController(concertProjector, concertStore);
        Model model = new ConcurrentModel();

        String viewName = controller.listConcerts(model);

        assertThat(viewName)
                .isEqualTo("event-viewer/concert-aggregates");
    }

    @Test
    void listConcertsAddsCorrectAttributesToModel() {
        var concertStore = EventStore.forConcerts();
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        ConcertId concertId = ConcertId.createRandom();
        String artist = "Test Artist";
        LocalDateTime showDateTime = LocalDateTime.of(2025, 7, 26, 20, 0);
        LocalTime doorsTime = LocalTime.of(19, 0);

        concertStore.save(ConcertFactory.scheduleConcertWith(concertId,
                                                             artist,
                                                             100,
                                                             showDateTime,
                                                             doorsTime));

        EventViewerController controller = new EventViewerController(concertProjector, concertStore);
        Model model = new ConcurrentModel();

        controller.listConcerts(model);

        List<ConcertListView> concertList = (List<ConcertListView>) model.getAttribute("concerts");
        assertThat(concertList)
                .hasSize(1)
                .extracting(ConcertListView::concertId, ConcertListView::artist)
                .containsExactly(tuple(concertId.id().toString(), artist));
    }

    @Test
    void showConcertEventsReturnsCorrectViewNameAndModelContents() {
        Fixture fixture = createAndSaveConcertWithMultipleEvents();

        ConcurrentModel model = new ConcurrentModel();
        String viewName = fixture.controller().showConcertEvents(fixture.concertIdAsString, 0, model);

        assertThat(viewName)
                .isEqualTo("event-viewer/concert-events");

        assertThat(model)
                .containsEntry("concertId", fixture.concertIdAsString);

        List<ConcertEvent> events = (List<ConcertEvent>) model.getAttribute("events");
        assertThat(events)
                .hasSize(2);

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
        Fixture fixture = createAndSaveConcertWithMultipleEvents();

        ConcurrentModel model = new ConcurrentModel();
        fixture.controller().showConcertEvents(fixture.concertIdAsString, 0, model);

        assertThat(model)
                .containsEntry("selectedIndex", 0);
    }

    @Test
    void selectedEventForShowConcertIsSelectedIndexFromQueryParam() {
        Fixture fixture = createAndSaveConcertWithMultipleEvents();

        ConcurrentModel model = new ConcurrentModel();
        fixture.controller().showConcertEvents(fixture.concertIdAsString,
                                               2,
                                               model);

        assertThat(model)
                .containsEntry("selectedIndex", 2);
    }

    private static Fixture createAndSaveConcertWithMultipleEvents() {
        var concertStore = EventStore.forConcerts();
        ConcertProjector concertProjector = new ConcertProjector(concertStore);

        ConcertId concertId = ConcertId.createRandom();
        Concert concert = scheduleAndRescheduleAndSave(concertId, concertStore);

        EventViewerController controller = new EventViewerController(concertProjector, concertStore);
        return new Fixture(concertId.id().toString(), concert, controller);
    }

    private record Fixture(String concertIdAsString, Concert concert, EventViewerController controller) {}

    private static Concert scheduleAndRescheduleAndSave(ConcertId concertId, EventStore<ConcertId, ConcertEvent, Concert> concertStore) {
        Concert concert = ConcertFactory.scheduleConcertWith(
                concertId,
                "Test Artist",
                100,
                LocalDateTime.of(2025, 7, 26, 20, 0),
                LocalTime.of(19, 0));
        concert.rescheduleTo(concert.showDateTime().plusMonths(2), concert.doorsTime());
        concertStore.save(concert);
        return concert;
    }

}
