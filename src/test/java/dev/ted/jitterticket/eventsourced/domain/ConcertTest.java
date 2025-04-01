package dev.ted.jitterticket.eventsourced.domain;

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
            String artist = "Test Artist";
            Concert concert = Concert.schedule(ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase, artist);

            List<ConcertEvent> events = concert.uncommittedEvents();

            assertThat(events)
                    .containsExactly(new ConcertScheduled(
                            ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase, artist
                    ));
        }

        @Test
        void rescheduleConcertGeneratesConcertRescheduled() {
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            ConcertScheduled concertScheduled = createConcertScheduledEvent(originalShowDateTime, originalDoorsTime);
            Concert concert = Concert.reconstitute(List.of(concertScheduled));

            LocalDateTime newShowDateTime = originalShowDateTime.plusDays(1).minusHours(1);
            LocalTime newDoorsTime = originalDoorsTime.minusHours(1);
            concert.rescheduleTo(newShowDateTime, newDoorsTime);

            assertThat(concert.uncommittedEvents())
                    .containsExactly(
                            new ConcertRescheduled(newShowDateTime, newDoorsTime));
        }

    }

    static ConcertScheduled createConcertScheduledEvent(LocalDateTime originalShowDateTime, LocalTime originalDoorsTime) {
        int ticketPrice = 35;
        int capacity = 100;
        int maxTicketsPerPurchase = 4;
        String artist = "Test Artist";
        return new ConcertScheduled(ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase, artist);
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
            String artist = "Test Artist";
            ConcertScheduled concertScheduled = new ConcertScheduled(ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase, artist);
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
            assertThat(concert.artist())
                    .isEqualTo(artist);

        }

        @Test
        void concertRescheduledUpdatesShowAndDoorTimesOnly() {
            int ticketPrice = 35;
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Test Artist";
            ConcertScheduled concertScheduled = new ConcertScheduled(ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase, artist);
            LocalDateTime newShowDateTime = originalShowDateTime.plusDays(1).minusHours(1);
            LocalTime newDoorsTime = originalDoorsTime.minusHours(1);
            ConcertRescheduled concertRescheduled = new ConcertRescheduled(newShowDateTime, newDoorsTime);

            List<ConcertEvent> concertEvents = List.of(concertScheduled,
                                                       concertRescheduled);

            Concert concert = Concert.reconstitute(concertEvents);

            assertThat(concert.showDateTime())
                    .as("Show date time was not updated")
                    .isEqualTo(newShowDateTime);
            assertThat(concert.doorsTime())
                    .as("Door time was not updated")
                    .isEqualTo(newDoorsTime);
            assertThat(concert.ticketPrice())
                    .as("Ticket Price should not have changed")
                    .isEqualTo(ticketPrice);
        }
    }

}
