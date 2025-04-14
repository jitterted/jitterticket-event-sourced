package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class PurchaseTicketsUseCaseTest {

    @Test
    void failureOfBuyTicketsReturnsEmptyOptional() {
        var concertStore = EventStore.forConcerts();
        PurchaseTicketsUseCase purchaseTicketsUseCase = new PurchaseTicketsUseCase(concertStore);
        CustomerId customerId = new CustomerId(UUID.randomUUID());
        ConcertId invalidConcertId = ConcertId.createRandom();

        Optional<TicketOrderId> ticketOrderIdOptional =
                purchaseTicketsUseCase.buyTickets(invalidConcertId, customerId, 1);

        assertThat(ticketOrderIdOptional)
                .as("Ticket order should have failed and therefore returned an empty TicketOrderId")
                .isEmpty();
    }

    @Test
    void buyTicketsReducesNumberOfTicketsAvailable() {
        var concertStore = EventStore.forConcerts();
        Concert concertBefore = ConcertFactory.createWithCapacity(100);
        concertStore.save(concertBefore);
        PurchaseTicketsUseCase purchaseTicketsUseCase = new PurchaseTicketsUseCase(concertStore);
        CustomerId customerId = new CustomerId(UUID.randomUUID());

        Optional<TicketOrderId> ticketOrderId = purchaseTicketsUseCase.buyTickets(concertBefore.getId(), customerId, 4);

        assertThat(ticketOrderId)
                .as("TicketOrderId should have been returned for a successful ticket purchase")
                .isPresent();
        Concert concertAfter = concertStore.findById(concertBefore.getId()).orElseThrow();
        assertThat(concertAfter.availableTicketCount())
                .isEqualTo(100 - 4);
    }

}
