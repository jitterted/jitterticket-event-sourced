package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.stream.Stream;

public interface DomainProjector<STATE, DELTA extends ProjectionDelta> {
    ProjectorResult<STATE, DELTA> project(STATE currentState, Stream<? extends Event> eventStream);

    record ProjectorResult<STATE, DELTA>(STATE fullState, DELTA delta) {}
}
