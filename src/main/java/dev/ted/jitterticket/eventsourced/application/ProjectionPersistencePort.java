package dev.ted.jitterticket.eventsourced.application;

public interface ProjectionPersistencePort<STATE> {
    record Snapshot<STATE>(STATE state, Checkpoint checkpoint) {}

    Snapshot<STATE> loadSnapshot();

    void saveDelta(STATE delta, Checkpoint newCheckpoint);
}
