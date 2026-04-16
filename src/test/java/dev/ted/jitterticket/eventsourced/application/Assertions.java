package dev.ted.jitterticket.eventsourced.application;

public class Assertions extends org.assertj.core.api.Assertions {

    public static <STATE> CheckpointedAssert<STATE> assertThat(Checkpointed<STATE> actual) {
        return new CheckpointedAssert<>(actual);
    }
    
}