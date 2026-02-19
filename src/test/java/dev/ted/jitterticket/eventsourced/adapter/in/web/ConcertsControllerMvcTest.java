package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ConcertsController.class)
class ConcertsControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void getToConcertsViewEndpointReturns200() {

        mvc.get()
           .uri("/concerts")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

}