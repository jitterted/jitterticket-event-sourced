package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.adapter.in.web.BaseMvcTest;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EventViewerController.class)
class EventViewerControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Test
    void getEventViewerProjectionChoicesReturns200Ok() {
        mvc.get()
           .uri("/event-viewer")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

    @Test
    void getConcertListEndpointReturns200Ok() {
        mvc.get()
           .uri("/event-viewer/concerts")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

    @Test
    void getConcertEventsEndpointReturns200Ok() {
        ConcertId concertId = ConcertFactory.Store.saveScheduledConcertIn(concertStore);
        mvc.get()
           .uri("/event-viewer/concerts/" + concertId.id())
           .assertThat()
           .hasStatus2xxSuccessful()
           .model().containsEntry("selectedEvent", 1L);
    }
}
