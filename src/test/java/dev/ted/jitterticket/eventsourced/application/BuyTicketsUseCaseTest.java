package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class BuyTicketsUseCaseTest {

    @Test
    void availableConcertsReturnsNoConcertsWhenNoneCreated() {
        BuyTicketsUseCase buyTicketsUseCase = new BuyTicketsUseCase(new ConcertStore());

        Stream<Concert> availableConcerts = buyTicketsUseCase.availableConcerts();

        assertThat(availableConcerts)
                .isEmpty();
    }

    @Test
    void availableConcertsReturnsTheSingleTicketableConcert() {
        Concert concert = Concert.schedule(99,
                                           LocalDateTime.now(),
                                           LocalTime.now().minusHours(1),
                                           100,
                                           4);
        ConcertStore concertStore = new ConcertStore();
        concertStore.save(concert);
        BuyTicketsUseCase buyTicketsUseCase = new BuyTicketsUseCase(concertStore);

        Stream<Concert> availableConcerts = buyTicketsUseCase.availableConcerts();

        assertThat(availableConcerts)
                .hasSize(1);
    }
}