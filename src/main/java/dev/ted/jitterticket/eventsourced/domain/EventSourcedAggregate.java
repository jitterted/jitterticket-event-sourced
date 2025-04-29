package dev.ted.jitterticket.eventsourced.domain;

import java.util.ArrayList;
import java.util.List;

public abstract class EventSourcedAggregate<EVENT extends Event, ID extends Id> {
    private final List<EVENT> uncommittedEvents = new ArrayList<>();
    protected int lastLoadedEventSequenceNumber = -1;
    private ID id;

    protected void enqueue(EVENT event) {
        uncommittedEvents.add(event);
        apply(event);
    }

    protected void applyAll(List<EVENT> loadedEvents) {
        loadedEvents.forEach(this::apply);
        lastLoadedEventSequenceNumber = loadedEvents.getLast().eventSequence();
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

    protected Integer nextEventSequenceNumber() {
        return ++lastLoadedEventSequenceNumber;
    }
}
