package dev.ted.jitterticket.eventsourced.application;

@FunctionalInterface
public interface Command<T> {
    void execute(T t);
}
