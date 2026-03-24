package dev.ted.jitterticket.eventsourced.application;

public class SchedulingConflictException extends RuntimeException {
    public SchedulingConflictException() {
        super();
    }

    public SchedulingConflictException(String message) {
        super(message);
    }
}
