package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Tag("mvc")
@Tag("spring")
@WebMvcTest(ConcertsController.class)
class ConcertsControllerMvcTest {

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