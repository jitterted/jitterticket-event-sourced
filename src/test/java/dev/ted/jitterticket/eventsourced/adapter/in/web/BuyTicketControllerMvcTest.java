package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@Tag("mvc")
@Tag("spring")
@WebMvcTest(BuyTicketController.class)
class BuyTicketControllerMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void getToBuyTicketViewEndpointReturns200() {

        mvc.get()
           .uri("/concerts/af05fc05-2de1-46d8-9568-01381029feb7")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

}