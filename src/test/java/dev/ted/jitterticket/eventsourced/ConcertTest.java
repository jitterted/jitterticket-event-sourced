package dev.ted.jitterticket.eventsourced;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ConcertTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        void scheduleConcertGeneratesConcertScheduled() {
            int ticketPrice = 35;
            LocalDateTime showDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime doorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            Concert concert = Concert.schedule(ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);

            List<ConcertEvent> events = concert.uncommittedEvents();

            assertThat(events)
                    .containsExactly(new ConcertScheduled(
                            ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
                    ));
        }

    }

    @Nested
    class EventsProjectState {

        @Test
        void concertScheduledUpdatesConcertDetails() {
            int ticketPrice = 35;
            LocalDateTime showDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime doorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            ConcertScheduled concertScheduled = new ConcertScheduled(ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
            List<ConcertEvent> concertEvents = List.of(concertScheduled);

            Concert concert = Concert.reconstitute(concertEvents);

            assertThat(concert.ticketPrice())
                    .isEqualTo(ticketPrice);
            assertThat(concert.showDateTime())
                    .isEqualTo(showDateTime);
            assertThat(concert.doorsTime())
                    .isEqualTo(doorsTime);
            assertThat(concert.capacity())
                    .isEqualTo(capacity);
            assertThat(concert.maxTicketsPerPurchase())
                    .isEqualTo(maxTicketsPerPurchase);

        }
    }

}
