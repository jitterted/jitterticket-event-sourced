package dev.ted.jitterticket.eventsourced.application;

public class InvalidParamsException extends RuntimeException {
    public InvalidParamsException() {
        super();
    }

    public InvalidParamsException(String message) {
        super(message);
    }
}
