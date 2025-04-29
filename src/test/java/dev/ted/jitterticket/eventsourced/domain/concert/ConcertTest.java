package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
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
        void scheduleConcertGeneratesConcertScheduledEventWithId() {
            int ticketPrice = 35;
            LocalDateTime showDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime doorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Headliner";
            ConcertId concertId = ConcertId.createRandom();
            Concert concert = Concert.schedule(concertId, artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);

            List<ConcertEvent> events = concert.uncommittedEvents();

            assertThat(events)
                    .containsExactly(new ConcertScheduled(
                            concertId, 0, artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
                    ));
        }

        @Test
        void rescheduleConcertGeneratesConcertRescheduled() {
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            ConcertScheduled concertScheduled = ConcertFactory.Events.scheduledConcert(originalShowDateTime, originalDoorsTime);
            Concert concert = Concert.reconstitute(List.of(concertScheduled));

            LocalDateTime newShowDateTime = originalShowDateTime.plusDays(1).minusHours(1);
            LocalTime newDoorsTime = originalDoorsTime.minusHours(1);
            concert.rescheduleTo(newShowDateTime, newDoorsTime);

            assertThat(concert.uncommittedEvents())
                    .containsExactly(
                            new ConcertRescheduled(concert.getId(),
                                                   1,
                                                   newShowDateTime,
                                                   newDoorsTime));
        }

        @Test
        void purchaseTicketsGeneratesTicketsSold() {
            int ticketPrice = 35;
            ConcertScheduled concertScheduled =
                    ConcertFactory.Events.scheduledConcertWithCapacityAndTicketPrice(100, ticketPrice);
            Concert concert = Concert.reconstitute(List.of(concertScheduled));
            ConcertId concertId = concert.getId();
            CustomerId customerId = CustomerId.createRandom();
            int quantity = 2;

            concert.sellTicketsTo(customerId, quantity);

            assertThat(concert.uncommittedEvents())
                    .containsExactly(
                            new TicketsSold(concertId, 1, quantity, -1)
                    );
        }

    }

    @Nested
    class EventsProjectState {

        @Test
        void concertScheduledUpdatesConcertDetailsWithId() {
            int ticketPrice = 35;
            LocalDateTime showDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime doorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Headliner";
            ConcertId concertId = ConcertId.createRandom();
            ConcertScheduled concertScheduled = new ConcertScheduled(concertId, 0, artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
            List<ConcertEvent> concertEvents = List.of(concertScheduled);

            Concert concert = Concert.reconstitute(concertEvents);

            assertThat(concert.getId())
                    .isEqualTo(concertId);
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
            assertThat(concert.availableTicketCount())
                    .isEqualTo(capacity);
        }

        @Test
        void concertRescheduledUpdatesShowAndDoorTimesOnly() {
            int ticketPrice = 35;
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            int capacity = 100;
            int maxTicketsPerPurchase = 4;
            String artist = "Rescheduler Artist Name";
            ConcertId concertId = ConcertId.createRandom();
            ConcertScheduled concertScheduled = new ConcertScheduled(concertId, 0, artist, ticketPrice, originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase);
            LocalDateTime newShowDateTime = originalShowDateTime.plusDays(1).minusHours(1);
            LocalTime newDoorsTime = originalDoorsTime.minusHours(1);
            ConcertRescheduled concertRescheduled = new ConcertRescheduled(concertId, 0, newShowDateTime, newDoorsTime);

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

        @Test
        void ticketsSoldUpdatesAvailableTicketCount() {
            ConcertScheduled concertScheduled = ConcertFactory.Events.scheduledConcertWithCapacityOf(100);
            int quantitySold = 6;
            TicketsSold ticketsSold = new TicketsSold(
                    concertScheduled.concertId(), 0, quantitySold, -1);

            Concert concert = Concert.reconstitute(List.of(concertScheduled,
                                                           ticketsSold));

            assertThat(concert.availableTicketCount())
                    .isEqualTo(100 - quantitySold);
        }

    }

}
