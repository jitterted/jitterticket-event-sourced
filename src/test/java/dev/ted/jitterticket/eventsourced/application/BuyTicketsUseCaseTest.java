package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import dev.ted.jitterticket.eventsourced.domain.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.CustomerId;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class BuyTicketsUseCaseTest {

    @Test
    void buyTicketsReducesNumberOfTicketsAvailable() {
        var concertStore = EventStore.forConcerts();
        Concert concertBefore = ConcertFactory.createWithCapacity(100);
        concertStore.save(concertBefore);
        BuyTicketsUseCase buyTicketsUseCase = new BuyTicketsUseCase(concertStore);
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        buyTicketsUseCase.buyTickets(concertBefore.getId(), customerId, 4);

        Concert concertAfter = concertStore.findById(concertBefore.getId()).orElseThrow();
        assertThat(concertAfter.availableTicketCount())
                .isEqualTo(100 - 4);
    }

}
