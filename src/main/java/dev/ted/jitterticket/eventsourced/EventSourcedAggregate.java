package dev.ted.jitterticket.eventsourced;

import java.util.ArrayList;
import java.util.List;

public abstract class EventSourcedAggregate {
    private final List<ConcertEvent> uncommittedEvents = new ArrayList<>();

    protected void enqueue(ConcertEvent concertEvent) {
        uncommittedEvents.add(concertEvent);
    }

    protected abstract void apply(ConcertEvent concertEvent);

    public List<ConcertEvent> uncommittedEvents() {
        return uncommittedEvents;
    }
}
