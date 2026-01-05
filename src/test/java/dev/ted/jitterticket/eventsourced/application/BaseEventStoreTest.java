package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertFactory;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class BaseEventStoreTest {

    @Test
    void saveSendsEventsToMultipleSubscribedEventConsumers() {
        EventStore<ConcertId, ConcertEvent, Concert> concertEventStore = InMemoryEventStore.forConcerts();
        AtomicInteger counter = new AtomicInteger(0);
        EventConsumer<ConcertEvent> eventConsumerCountingSpy =
                concertEventStream -> {
                    //noinspection ResultOfMethodCallIgnored
                    concertEventStream.toList(); // consume the event stream to emulate what a real event consumer does
                    counter.incrementAndGet();
                };
        concertEventStore.subscribe(eventConsumerCountingSpy);
        concertEventStore.subscribe(eventConsumerCountingSpy);

        concertEventStore.save(ConcertFactory.createConcert());

        assertThat(counter)
                .as("Should count 2 times, 1 for each subscribed event consumer")
                .hasValue(2);
    }
}