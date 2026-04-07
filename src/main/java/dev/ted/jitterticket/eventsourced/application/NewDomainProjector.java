package dev.ted.jitterticket.eventsourced.application;

public abstract class NewDomainProjector<STATE, DELTA extends ProjectionDelta>
        extends EventHandler {

    public abstract STATE currentState();

    /**
     * Returns any uncommitted changes in the projection,
     * clearing it as a result.
     *
     * @return uncommitted changes since the last time flush was called,
     * or since the projector was instantiated
     */
    public abstract DELTA flush();

    /**
     * Checkpoint for the most recent event sequence processed by this projector.
     *
     * @return the last event sequence processed, or Checkpoint.INITIAL if none have been processed
     */
    public abstract Checkpoint checkpoint();
}
