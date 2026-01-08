package dev.ted.jitterticket.eventsourced.application;

import java.util.Objects;

public class Checkpoint {
    public static final Checkpoint INITIAL = new Checkpoint(0);

    private final long value;

    private Checkpoint(long value) {
        this.value = value;
    }

    public static Checkpoint of(long value) {
        if (value == 0) {
            throw new IllegalArgumentException("Checkpoint value must be 1 or more, or use the constant Checkpoint.INITIAL for 0.");
        }
        if (value < 1) {
            throw new IllegalArgumentException("Checkpoint value must be 1 or more.");
        }
        return new Checkpoint(value);
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Checkpoint that = (Checkpoint) o;
        return value == that.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return "Checkpoint[value=" + value + "]";
    }
}
