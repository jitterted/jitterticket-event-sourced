package dev.ted.jitterticket.eventsourced.adapter.in.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SalesController.class)
class SalesControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Test
    void getToSalesViewEndpointReturns200() {

        mvc.get()
           .uri("/concert-sales")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

}