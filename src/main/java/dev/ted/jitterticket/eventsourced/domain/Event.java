package dev.ted.jitterticket.eventsourced.domain;

public abstract class Event {
    private Long eventSequence;

    protected Event(Long eventSequence) {
        this.eventSequence = eventSequence;
    }

    public Long eventSequence() {
        return eventSequence;
    }

    public void setEventSequence(Long eventSequence) {
        this.eventSequence = eventSequence;
    }
}
