package dev.ted.jitterticket.eventsourced.application;

public record Checkpointed<STATE>(STATE state, Checkpoint checkpoint) {
}
