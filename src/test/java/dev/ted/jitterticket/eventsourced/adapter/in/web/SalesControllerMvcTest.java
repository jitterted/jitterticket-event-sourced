package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(SalesController.class)
class SalesControllerMvcTest extends BaseMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Disabled("Until we've finished the restructure of the projector")
    @Test
    void getToSalesViewEndpointReturns200() {
        Concert concert = ConcertFactory.createConcertWithArtist("Jittery Pigs");
        concertStore.save(concert);

        mvc.get()
           .uri("/concert-sales")
           .assertThat()
           .hasStatus2xxSuccessful()
           .bodyText()
           .contains("<h1>Concert Sales Report</h1>",
                     "<td>Jittery Pigs</td>")
        ;
    }

}