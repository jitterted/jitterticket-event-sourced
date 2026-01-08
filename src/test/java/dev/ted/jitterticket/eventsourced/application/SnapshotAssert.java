package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.application.ProjectionPersistencePort.Snapshot;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ObjectAssert;

public class SnapshotAssert<STATE> extends AbstractAssert<SnapshotAssert<STATE>, Snapshot<STATE>> {

    protected SnapshotAssert(Snapshot<STATE> actual) {
        super(actual, SnapshotAssert.class);
    }

    public static <STATE> SnapshotAssert<STATE> assertThat(Snapshot<STATE> actual) {
        return new SnapshotAssert<>(actual);
    }

    public SnapshotAssert<STATE> hasCheckpointOf(long expectedCheckpoint) {
        isNotNull();
        if (actual.checkpoint() != expectedCheckpoint) {
            failWithMessage("Expected snapshot to have checkpoint <%s> but was <%s>",
                            expectedCheckpoint, actual.checkpoint());
        }
        return this;
    }

    /**
     * Assumes STATE has a way to check for emptiness (e.g., via a custom method or common interface).
     */
    public SnapshotAssert<STATE> hasEmptyState() {
        isNotNull();
        // Assuming RegisteredCustomers or similar has a hasData() method
        if (actual.state() instanceof RegisteredCustomers rc) {
            if (rc.hasData()) {
                failWithMessage("Expected snapshot state to be empty, but it contained data: <%s>", rc);
            }
        } else {
            failWithMessage("Don't know how to check the emptiness of state of type %s", actual.state().getClass().getSimpleName());
        }
        return this;
    }

    public ObjectAssert<STATE> state() {
        isNotNull();
        return new ObjectAssert<>(actual.state());
    }


}