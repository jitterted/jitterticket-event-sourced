package dev.ted.jitterticket.eventsourced.application;

public interface ProjectionPersistencePort<STATE, DELTA extends ProjectionDelta> {

    Checkpointed<STATE> loadSnapshot();

    NewDomainProjector<STATE, DELTA> loadProjector();

    void saveDelta(Checkpointed<DELTA> checkpointed);
}
