package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertProjector;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
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
    void showConcertEventsReturnsCorrectViewNameWithAllEvents() {
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

        String viewName = controller.showConcertEvents(concertId.id().toString(), model);

        assertThat(viewName)
                .isEqualTo("event-viewer/concert-events");

        assertThat(model.getAttribute("concertId"))
                .isEqualTo(concertId.id().toString());

        List<ConcertEvent> events = (List<ConcertEvent>) model.getAttribute("events");
        assertThat(events)
                .hasSize(1);

    }

}
