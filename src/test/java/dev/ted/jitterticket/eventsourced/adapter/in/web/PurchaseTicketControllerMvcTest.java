package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.TixConfiguration;
import dev.ted.jitterticket.eventsourced.application.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Disabled;
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
@WebMvcTest(PurchaseTicketController.class)
@Import(TixConfiguration.class)
class PurchaseTicketControllerMvcTest {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    EventStore<ConcertId, ConcertEvent, Concert> concertStore;

    @Test
    void getToBuyTicketViewEndpointReturns200Ok() {
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

    @Test
    @Disabled("dev.ted.jitterticket.eventsourced.adapter.in.web.PurchaseTicketControllerMvcTest 4/14/25 12:32 — need to send a ticket order form along with the id")
    void postToBuyTicketEndpointRedirects() {
        ConcertId concertId = new ConcertId(UUID.randomUUID());
        mvc.post()
           .uri("/concerts/" + concertId.id().toString())
           .assertThat()
           .hasStatus3xxRedirection();
    }
}