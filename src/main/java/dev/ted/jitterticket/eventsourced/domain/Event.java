package dev.ted.jitterticket.eventsourced.domain;

public abstract class Event {
    private final Integer eventSequence;

    protected Event(Integer eventSequence) {
        this.eventSequence = eventSequence;
    }

    public Integer eventSequence() {
        return eventSequence;
    }
}
