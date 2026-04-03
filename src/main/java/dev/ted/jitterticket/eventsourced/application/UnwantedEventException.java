package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.Set;
import java.util.stream.Collectors;

public class UnwantedEventException extends RuntimeException {
    public UnwantedEventException() {
        super();
    }

    public UnwantedEventException(Event unwantedEvent, Set<Class<? extends Event>> wantedEvents) {
        super("Unwanted event %s received, this class only accepts: %s"
                      .formatted(unwantedEvent.getClass().toString(),
                                 wantedEvents.stream()
                                             .map(Class::getSimpleName)
                                             .sorted().collect(Collectors.joining(","))));
    }
}
