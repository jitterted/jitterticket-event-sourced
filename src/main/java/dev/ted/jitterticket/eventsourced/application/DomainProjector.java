package dev.ted.jitterticket.eventsourced.application;

import java.util.stream.Stream;

public interface DomainProjector<EVENT, STATE, DELTA extends ProjectionDelta> {
    ProjectorResult<STATE, DELTA> project(STATE currentState, Stream<EVENT> eventStream);

    record ProjectorResult<STATE, DELTA>(STATE fullState, DELTA delta) {}
}
