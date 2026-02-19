package dev.ted.jitterticket.eventsourced.domain.concert;

import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.Commands;
import dev.ted.jitterticket.eventsourced.application.InMemoryEventStore;
import dev.ted.jitterticket.eventsourced.application.LocalDateTimeFactory;
import dev.ted.jitterticket.eventsourced.application.RescheduleParams;
import dev.ted.jitterticket.eventsourced.application.ScheduleParams;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

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

            Stream<ConcertEvent> events = concert.uncommittedEvents();

            assertThat(events)
                    .containsExactly(new ConcertScheduled(
                            concertId, null, artist, ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase
                    ));
        }

        @Test
        void wrappedRescheduleCommandReschedulesConcertWhenExecutedWithParams() {
            EventStore<ConcertId, ConcertEvent, Concert> concertEventStore = InMemoryEventStore.forConcerts();
            ConcertId concertId = ConcertId.createRandom();
            concertEventStore.save(ConcertFactory.createConcertWithShowDateTimeOf(concertId, LocalDateTimeFactory.withNow().oneWeekInTheFutureAtMidnight()));
            CommandExecutorFactory commandExecutorFactory = CommandExecutorFactory.create(concertEventStore);

            var command = commandExecutorFactory.wrapWithParams(
                    (concert, reschedule) ->
                            concert.rescheduleTo(
                                    reschedule.showDateTime(),
                                    reschedule.doorsTime()));

            LocalDateTime newShowDateTime = LocalDateTimeFactory.withNow().oneMonthInTheFutureAtMidnight();
            RescheduleParams rescheduleParams = new RescheduleParams(
                    newShowDateTime,
                    LocalTime.of(20, 0));

            command.execute(concertId, rescheduleParams);

            assertThat(concertEventStore.findById(concertId))
                    .get()
                    .extracting(Concert::showDateTime)
                    .isEqualTo(newShowDateTime);
        }

        @Test
        void wrappedScheduleCommandCreatesConcertWithParams() {
            EventStore<ConcertId, ConcertEvent, Concert> concertEventStore = InMemoryEventStore.forConcerts();
            CommandExecutorFactory commandExecutorFactory = CommandExecutorFactory.create(concertEventStore);
            Commands commands = new Commands(commandExecutorFactory);

            ScheduleParams scheduleParams = new ScheduleParams(
                    "Headliner", 35, LocalDateTime.of(2025, 11, 11, 20, 0),
                    LocalTime.of(19, 0), 100, 4);
            ConcertId createdConcertId = commands.createScheduleCommand().execute(scheduleParams);

            assertThat(createdConcertId)
                    .isNotNull();
            assertThat(concertEventStore.findById(createdConcertId))
                    .isPresent()
                    .get()
                    .usingRecursiveComparison()
                    .ignoringFields("id", "uncommittedEvents")
                    .isEqualTo(Concert.schedule(
                            null, "Headliner", 35,
                            LocalDateTime.of(2025, 11, 11, 20, 0),
                            LocalTime.of(19, 0),
                            100, 4));

        }

        @Test
        void rescheduleConcertGeneratesConcertRescheduled() {
            LocalDateTime originalShowDateTime = LocalDateTime.of(2025, 11, 11, 20, 0);
            LocalTime originalDoorsTime = LocalTime.of(19, 0);
            ConcertScheduled concertScheduled =
                    ConcertFactory.Events.
                            scheduledConcert(originalShowDateTime, originalDoorsTime);
            Concert concert = Concert.reconstitute(List.of(concertScheduled));

            LocalDateTime newShowDateTime = originalShowDateTime.plusDays(1).minusHours(1);
            LocalTime newDoorsTime = originalDoorsTime.minusHours(1);
            concert.rescheduleTo(newShowDateTime, newDoorsTime);

            assertThat(concert.uncommittedEvents())
                    .containsExactly(new ConcertRescheduled(
                            concert.getId(),
                            null,
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
                            new TicketsSold(concertId,
                                            null,
                                            2,
                                            35 * 2)
                    );
        }

        @Test
        void stopTicketSalesGeneratesTicketSalesStopped() {
            Concert concert = Concert.reconstitute(List.of(ConcertFactory.Events.scheduledConcert()));

            concert.stopTicketSales();

            assertThat(concert.uncommittedEvents())
                    .containsExactly(
                            new TicketSalesStopped(concert.getId(), null));
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
            ConcertScheduled concertScheduled = new ConcertScheduled(concertId, 1L, artist,
                                                                     ticketPrice, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);
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
            assertThat(concert.canSellTickets())
                    .as("For newly scheduled concerts canSellTickets should be TRUE")
                    .isTrue();
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
            ConcertScheduled concertScheduled = new ConcertScheduled(concertId, 1L, artist, ticketPrice,
                                                                     originalShowDateTime, originalDoorsTime, capacity, maxTicketsPerPurchase);
            LocalDateTime newShowDateTime = originalShowDateTime.plusDays(1).minusHours(1);
            LocalTime newDoorsTime = originalDoorsTime.minusHours(1);
            ConcertRescheduled concertRescheduled = new ConcertRescheduled(concertId, 2L,
                                                                           newShowDateTime, newDoorsTime);

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
            ConcertScheduled concertScheduled =
                    ConcertFactory.Events.scheduledConcertWithCapacityOf(100);
            int quantitySold = 6;
            TicketsSold ticketsSold = new TicketsSold(
                    concertScheduled.concertId(), 1L, quantitySold, -1);

            Concert concert = Concert.reconstitute(List.of(concertScheduled,
                                                           ticketsSold));

            assertThat(concert.availableTicketCount())
                    .isEqualTo(100 - quantitySold);
        }

        @Test
        void ticketSalesStoppedDisablesCanSellTickets() {
            ConcertScheduled concertScheduled = ConcertFactory.Events.scheduledConcert();

            TicketSalesStopped ticketSalesStopped = new TicketSalesStopped(
                    concertScheduled.concertId(), 42L);
            Concert concert = Concert.reconstitute(
                    List.of(concertScheduled,
                            ticketSalesStopped));

            assertThat(concert.canSellTickets())
                    .as("Applying TicketSalesStopped must change canSellTickets to be FALSE")
                    .isFalse();
        }
    }

}
