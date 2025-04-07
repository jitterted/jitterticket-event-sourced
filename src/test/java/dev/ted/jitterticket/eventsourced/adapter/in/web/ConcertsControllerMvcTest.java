package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Tag("mvc")
class ConcertsControllerMvcTest {

    @Test
    void getToConcertsViewEndpointReturns200() {

        MockMvcTester mvc = MockMvcTester.of(new ConcertsController());

        mvc.get()
           .uri("/concerts")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

}