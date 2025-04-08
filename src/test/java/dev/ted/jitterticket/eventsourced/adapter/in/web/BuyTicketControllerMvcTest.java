package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.TixConfiguration;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.ConcertId;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Tag("mvc")
@Tag("spring")
@WebMvcTest(BuyTicketController.class)
@Import(TixConfiguration.class)
class BuyTicketControllerMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Test
    void getToBuyTicketViewEndpointReturns200() {
        ConcertId concertId = new ConcertId(UUID.randomUUID());
        concertStore.save(Concert.schedule(
                concertId,
                "Blue Note Quartet",
                35,
                LocalDateTime.of(2025, 8, 22, 19, 30),
                LocalTime.of(18, 30),
                75,
                2));

        mvc.get()
           .uri("/concerts/" + concertId.id().toString())
           .assertThat()
           .hasStatus2xxSuccessful();
    }

}