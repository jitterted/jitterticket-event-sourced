package dev.ted.jitterticket.eventsourced.application;

import java.util.stream.Stream;

public interface DomainProjector<EVENT, STATE> {
    ProjectorResult<STATE> project(STATE currentState, Stream<EVENT> eventStream);

    record ProjectorResult<STATE>(STATE fullState, STATE delta) {}
}
