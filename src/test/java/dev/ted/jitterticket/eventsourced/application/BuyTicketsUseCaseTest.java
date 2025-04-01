package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Concert;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class BuyTicketsUseCaseTest {

    @Test
    void availableConcertsReturnsNoConcertsWhenNoneCreated() {
        BuyTicketsUseCase buyTicketsUseCase = new BuyTicketsUseCase();

        Stream<Concert> availableConcerts = buyTicketsUseCase.availableConcerts();

        assertThat(availableConcerts)
                .isEmpty();
    }

    @Test
    void availableConcertsReturnsTheSingleTicketableConcert() {
        // create Concert
        // create BuyTicketsUseCase
        // buyTicketsUseCase.availableConcerts()
        // -> the Concert
    }
}