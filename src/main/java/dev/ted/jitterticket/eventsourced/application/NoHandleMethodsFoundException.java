package dev.ted.jitterticket.eventsourced.application;

public class NoHandleMethodsFoundException extends RuntimeException {
    public NoHandleMethodsFoundException(String message) {
        super(message);
    }
}
