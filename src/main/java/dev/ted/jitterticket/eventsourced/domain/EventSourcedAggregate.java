package dev.ted.jitterticket.eventsourced.domain;

import java.util.ArrayList;
import java.util.List;

public abstract class EventSourcedAggregate<EVENT extends Event, ID extends Id> {
    private final List<EVENT> uncommittedEvents = new ArrayList<>();
    protected long maxEventSequenceNumber = 0L;
    private ID id;

    protected void enqueue(EVENT event) {
        uncommittedEvents.add(event);
        apply(event);
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

    protected Long nextEventSequenceNumber() {
        return maxEventSequenceNumber++;
    }
}
