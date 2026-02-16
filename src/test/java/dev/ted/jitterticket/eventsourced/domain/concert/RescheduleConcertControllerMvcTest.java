package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.adapter.in.web.BaseMvcTest;
import dev.ted.jitterticket.eventsourced.adapter.in.web.RescheduleConcertController;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(RescheduleConcertController.class)
class RescheduleConcertControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Test
    void getToRescheduleEndpointReturns200Ok() {
        ConcertId concertId = ConcertFactory.Store
                .saveScheduledConcertIn(concertStore);

        mvc.get()
           .uri("/reschedule/" + concertId.id().toString())
           .assertThat()
           .hasStatus2xxSuccessful();
    }

    @Test
    void postToRescheduleEndpointReturns3xxRedirect() {
        ConcertId concertId = ConcertFactory.Store
                .saveScheduledConcertIn(concertStore);

        mvc.post()
           .uri("/reschedule/" + concertId.id().toString())
           .param("newShowDate", "2026-03-15")
           .param("newShowTime", "19:00")
           .param("newDoorsTime", "18:00")
           .assertThat()
           .hasStatus3xxRedirection();
    }
}
