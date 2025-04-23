package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.TixConfiguration;
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

    @Test
    void getConcertListEndpointReturns200Ok() {
        mvc.get()
           .uri("/event-viewer")
           .assertThat()
           .hasStatus2xxSuccessful();
    }
}
