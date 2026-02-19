package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(ScheduleConcertController.class)
class ScheduleConcertControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Test
    void getToScheduleEndpointReturns200Ok() {
        mvc.get()
           .uri("/schedule")
           .assertThat()
           .hasStatus2xxSuccessful();
    }

    @Test
    void postToRescheduleEndpointReturns3xxRedirect() {
        mvc.post()
           .uri("/schedule")
           .param("artist", "Spotlight")
           .param("ticketPrice", "45")
           .param("showDate", "2026-03-14")
           .param("showTime", "19:00")
           .param("doorsTime", "18:00")
           .param("maxCapacity", "100")
           .param("maxPerPurchase", "4")
           .assertThat()
           .hasStatus3xxRedirection();
    }
}
