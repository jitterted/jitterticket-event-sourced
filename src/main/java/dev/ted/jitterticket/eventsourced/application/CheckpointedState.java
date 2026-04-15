package dev.ted.jitterticket.eventsourced.application;

public record CheckpointedState<STATE>(STATE state, Checkpoint checkpoint) {
}
