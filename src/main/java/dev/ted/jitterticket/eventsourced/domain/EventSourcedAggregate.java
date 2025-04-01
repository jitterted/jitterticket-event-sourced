package dev.ted.jitterticket.eventsourced.domain;

import java.util.ArrayList;
import java.util.List;

public abstract class EventSourcedAggregate<EVENT, ID> {
    private final List<EVENT> uncommittedEvents = new ArrayList<>();
    private ID id;

    protected void enqueue(EVENT event) {
        uncommittedEvents.add(event);
    }

    protected abstract void apply(EVENT event);

    public List<EVENT> uncommittedEvents() {
        return uncommittedEvents;
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }
}
