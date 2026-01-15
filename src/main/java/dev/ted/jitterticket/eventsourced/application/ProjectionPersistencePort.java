package dev.ted.jitterticket.eventsourced.application;

public interface ProjectionPersistencePort<STATE, DELTA extends ProjectionDelta> {
    record Snapshot<STATE>(STATE state, Checkpoint checkpoint) {}

    Snapshot<STATE> loadSnapshot();

    void saveDelta(DELTA delta, Checkpoint newCheckpoint);
}
