package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.stream.Stream;

public interface EventStreamConsumer {
    void handle(Stream<? extends Event> eventStream);
}
