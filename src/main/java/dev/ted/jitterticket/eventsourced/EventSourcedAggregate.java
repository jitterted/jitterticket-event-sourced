package dev.ted.jitterticket.eventsourced;

import java.util.ArrayList;
import java.util.List;

public abstract class EventSourcedAggregate<EVENT> {
    private final List<EVENT> uncommittedEvents = new ArrayList<>();

    protected void enqueue(EVENT event) {
        uncommittedEvents.add(event);
    }

    protected abstract void apply(EVENT event);

    public List<EVENT> uncommittedEvents() {
        return uncommittedEvents;
    }
}
