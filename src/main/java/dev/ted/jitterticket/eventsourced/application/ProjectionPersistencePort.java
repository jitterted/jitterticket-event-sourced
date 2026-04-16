package dev.ted.jitterticket.eventsourced.application;

public interface ProjectionPersistencePort<STATE, DELTA extends ProjectionDelta> {

    // TO RETURN: NewDomainProjector<STATE, DELTA extends ProjectionDelta>
    Checkpointed<STATE> loadSnapshot();

    void saveDelta(DELTA delta, Checkpoint newCheckpoint);
}
