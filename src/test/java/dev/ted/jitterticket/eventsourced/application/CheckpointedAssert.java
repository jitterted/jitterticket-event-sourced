package dev.ted.jitterticket.eventsourced.application;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.ObjectAssert;

public class CheckpointedAssert<STATE> extends
        AbstractAssert<CheckpointedAssert<STATE>, Checkpointed<STATE>> {

    protected CheckpointedAssert(Checkpointed<STATE> actual) {
        super(actual, CheckpointedAssert.class);
    }

    public static <STATE> CheckpointedAssert<STATE> assertThat(Checkpointed<STATE> actual) {
        return new CheckpointedAssert<>(actual);
    }

    public CheckpointedAssert<STATE> hasCheckpointValueOf(long expectedCheckpointValue) {
        isNotNull();
        if (actual.checkpoint().value() != expectedCheckpointValue) {
            failWithMessage("Expected snapshot to have checkpoint <%s> but was <%s>",
                            expectedCheckpointValue, actual.checkpoint().value());
        }
        return this;
    }

    public CheckpointedAssert<STATE> checkpointIsEqualTo(Checkpoint expectedCheckpoint) {
        isNotNull();
        if (!actual.checkpoint().equals(expectedCheckpoint)) {
            failWithMessage("Expected snapshot to have checkpoint <%s> but was <%s>",
                            expectedCheckpoint, actual.checkpoint());
        }
        return this;
    }

    /**
     * Assumes STATE has a way to check for emptiness (e.g., via a custom method or common interface).
     */
    public CheckpointedAssert<STATE> hasEmptyState() {
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