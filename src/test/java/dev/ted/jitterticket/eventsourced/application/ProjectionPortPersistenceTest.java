package dev.ted.jitterticket.eventsourced.application;

import org.junit.jupiter.api.Test;

import static dev.ted.jitterticket.eventsourced.application.Assertions.assertThat;

class ProjectionPortPersistenceTest {

    @Test
    void emptyPersistenceLoadsEmptySnapshotWithCheckpointAtZero() {
        ProjectionPersistencePort<RegisteredCustomers> projectionPersistence =
                new MemoryRegisteredCustomersProjectionPersistence();

        var snapshot = projectionPersistence.loadSnapshot();

        assertThat(snapshot)
                .hasCheckpointOf(0)
                .hasEmptyState();
    }

}