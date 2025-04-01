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
            String artist = "Headliner";
            Concert concert = Concert.schedule(artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);

            List<ConcertEvent> events = concert.uncommittedEvents();

            assertThat(events)
                    .containsExactly(new ConcertScheduled(
                            artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
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
        String artist = "Irrelevant Artist Name";
        return new ConcertScheduled(artist, ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase);
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
            String artist = "Headliner";
            ConcertScheduled concertScheduled = new ConcertScheduled(artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
            List<ConcertEvent> concertEvents = List.of(concertScheduled);

            Concert concert = Concert.reconstitute(concertEvents);

            assertThat(concert.artist())
                    .isEqualTo(artist);
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

        @Test
        void concertRescheduledUpdatesShowAndDoorTimesOnly() {
            int ticketPrice = 35;
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Rescheduler Artist Name";
            ConcertScheduled concertScheduled = new ConcertScheduled(artist, ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase);
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
            assertThat(concert.artist())
                    .as("Artist should not have changed")
                    .isEqualTo(artist);
        }
    }

}
