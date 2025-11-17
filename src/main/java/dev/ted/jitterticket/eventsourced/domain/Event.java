package dev.ted.jitterticket.eventsourced.domain;

public abstract class Event {
    private final Integer eventSequence;
    private final Long globalEventSequence;

    protected Event(Integer eventSequence) {
        this(eventSequence, null);
    }

    protected Event(Integer eventSequence, Long globalEventSequence) {
        this.eventSequence = eventSequence;
        this.globalEventSequence = globalEventSequence;
    }

    public Integer eventSequence() {
        return eventSequence;
    }

    public Long globalEventSequence() {
        return globalEventSequence;
    }
}
