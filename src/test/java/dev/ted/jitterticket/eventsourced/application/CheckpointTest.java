package dev.ted.jitterticket.eventsourced.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class CheckpointTest {

    @Test
    void initialCheckpointIsZero() {
        assertThat(Checkpoint.INITIAL.value())
                .isZero();
    }

    @Test
    void checkpointWithValueOfOneOrMoreIsValid() {
        Checkpoint checkpoint = Checkpoint.of(1);
        assertThat(checkpoint.value())
                .isEqualTo(1);
    }

    @Test
    void checkpointWithValueLessThanOneAndNotInitialIsIllegal() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Checkpoint.of(-1))
                .withMessage("Checkpoint value must be 1 or more.");
    }

    @Test
    void checkpointWithValueOfZeroViaOfIsIllegal() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Checkpoint.of(0))
                .withMessage("Checkpoint value must be 1 or more, or use the constant Checkpoint.INITIAL for 0.");
    }
}
