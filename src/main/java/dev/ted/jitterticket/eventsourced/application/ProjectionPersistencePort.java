package dev.ted.jitterticket.eventsourced.application;

public interface ProjectionPersistencePort<STATE> {
    record Snapshot<STATE>(STATE state, long checkpoint) {}

    Snapshot<STATE> loadSnapshot();

    void saveDelta(STATE delta, long newCheckpoint);
}
