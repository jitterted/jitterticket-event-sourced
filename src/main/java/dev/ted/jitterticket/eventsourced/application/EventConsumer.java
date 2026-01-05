package dev.ted.jitterticket.eventsourced.application;

import java.util.stream.Stream;

public interface EventConsumer<EVENT> {
    void handle(Stream<EVENT> eventStream);
}
