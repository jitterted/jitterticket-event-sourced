package dev.ted.jitterticket.eventsourced.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public abstract class EventSourcedAggregate<EVENT extends Event, ID extends Id> {
    private ID id;
    private final List<EVENT> uncommittedEvents = new ArrayList<>();

    protected void enqueue(EVENT event) {
        uncommittedEvents.add(event);
        apply(event);
    }

    protected void applyAll(List<EVENT> loadedEvents) {
        loadedEvents.forEach(this::apply);
    }

    protected abstract void apply(EVENT event);

    /**
     * Returns events that have been generated since this object was loaded into memory or when this method was last called.
     * It's automatically cleared once the list of events is returned.
     *
     * @return events since this object was loaded, or the last time this was called
     */
    public Stream<EVENT> uncommittedEvents() {
        List<EVENT> events = List.copyOf(uncommittedEvents);
        uncommittedEvents.clear();
        return events.stream();
    }

    public ID getId() {
        return id;
    }

    public void setId(ID id) {
        this.id = id;
    }

}
