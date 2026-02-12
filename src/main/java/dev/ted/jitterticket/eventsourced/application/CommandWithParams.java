package dev.ted.jitterticket.eventsourced.application;

@FunctionalInterface
public interface CommandWithParams<AGGREGATE, PARAMS> {
    void execute(AGGREGATE aggregate, PARAMS params);
}
