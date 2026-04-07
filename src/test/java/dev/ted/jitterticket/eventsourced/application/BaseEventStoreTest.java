package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

class BaseEventStoreTest {

    @Test
    void saveSendsEventsToMultipleSubscribedEventConsumers() {
        EventStore<ConcertId, ConcertEvent, Concert> concertEventStore = InMemoryEventStore.forConcerts();
        AtomicInteger counter = new AtomicInteger(0);
        EventStreamConsumer eventStreamConsumerCountingSpy =
                concertEventStream -> {
                    //noinspection ResultOfMethodCallIgnored
                    concertEventStream.toList(); // consume the event stream to emulate what a real event consumer does
                    counter.incrementAndGet();
                };
        concertEventStore.subscribe(eventStreamConsumerCountingSpy,
                                    Set.of(ConcertScheduled.class));
        concertEventStore.subscribe(eventStreamConsumerCountingSpy,
                                    Set.of(ConcertScheduled.class));

        concertEventStore.save(ConcertFactory.createConcert());

        assertThat(counter)
                .as("Should count 2 times, 1 for each subscribed event consumer")
                .hasValue(2);
    }

    @Test
    void subscribersOnlyReceivedDesiredEventTypes() {
        EventStore<ConcertId, ConcertEvent, Concert> eventStore = InMemoryEventStore.forConcerts();
        MockEventStreamConsumer concertScheduledSubscriber =
                new MockEventStreamConsumer(ConcertScheduled.class);
        MockEventStreamConsumer ticketsSoldSubscriber =
                new MockEventStreamConsumer(TicketsSold.class);
        MockEventStreamConsumer ticketSalesStoppedSubscriber =
                new MockEventStreamConsumer(TicketSalesStopped.class);
        eventStore.subscribe(concertScheduledSubscriber,
                             Set.of(ConcertScheduled.class));
        eventStore.subscribe(ticketsSoldSubscriber,
                             Set.of(TicketsSold.class));
        eventStore.subscribe(ticketSalesStoppedSubscriber,
                             Set.of(TicketSalesStopped.class));

        Concert concert = ConcertFactory.createConcert();
        concert.rescheduleTo(LocalDateTime.now().plusMonths(1),
                             LocalTime.now());
        concert.sellTicketsTo(CustomerId.createRandom(), 2);
        eventStore.save(concert);

        concertScheduledSubscriber.verifyHandleInvoked();
        ticketsSoldSubscriber.verifyHandleInvoked();
        ticketSalesStoppedSubscriber.verifyHandleNotInvoked();
    }

    private static class MockEventStreamConsumer implements EventStreamConsumer {

        private final Class<? extends Event> desiredEventClass;
        private boolean handleStreamInvoked = false;

        public MockEventStreamConsumer(Class<? extends Event> desiredEventClass) {
            this.desiredEventClass = desiredEventClass;
        }

        @Override
        public void handle(Stream<? extends Event> eventStream) {
            handleStreamInvoked = true;
            assertThat(eventStream)
                    .as("Expected only events of type " + desiredEventClass)
                    .allMatch((Predicate<Event>) event ->
                            event.getClass().equals(desiredEventClass));
        }

        void verifyHandleInvoked() {
            assertThat(handleStreamInvoked)
                    .as("handle(eventStream) should have been invoked at least once.")
                    .isTrue();
        }

        public void verifyHandleNotInvoked() {
            assertThat(handleStreamInvoked)
                    .as("handle(eventStream) should NOT have been invoked.")
                    .isFalse();
        }
    }
}