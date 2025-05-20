package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.TixConfiguration;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Tag("mvc")
@Tag("spring")
@WebMvcTest(EventViewerController.class)
@Import(TixConfiguration.class)
class EventViewerControllerMvcTest {

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
        ConcertId concertId = ConcertFactory.Store.createSavedConcertIn(concertStore);
        mvc.get()
           .uri("/event-viewer/concerts/" + concertId.id())
           .assertThat()
           .hasStatus2xxSuccessful()
           .model().containsEntry("selectedEvent", 0);
    }
}
