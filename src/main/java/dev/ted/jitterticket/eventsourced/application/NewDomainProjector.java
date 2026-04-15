package dev.ted.jitterticket.eventsourced.application;

public abstract class NewDomainProjector<STATE, DELTA extends ProjectionDelta>
        extends EventHandler {

    /**
     * Returns the full state of the Projection calculated
     * by this Projector
     *
     * @return the full projection
     */
    public abstract STATE currentState();

    /**
     * Returns any uncommitted changes in the projection,
     * clearing it as a result.
     *
     * @return uncommitted changes since the last time flush was called,
     * or since the projector was instantiated
     */
    public abstract CheckpointedState<DELTA> flush();

}
