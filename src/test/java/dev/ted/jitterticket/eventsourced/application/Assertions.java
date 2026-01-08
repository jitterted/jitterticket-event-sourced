package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.ProjectionPersistencePort.Snapshot;

public class Assertions extends org.assertj.core.api.Assertions {

    public static <STATE> SnapshotAssert<STATE> assertThat(Snapshot<STATE> actual) {
        return new SnapshotAssert<>(actual);
    }
    
}