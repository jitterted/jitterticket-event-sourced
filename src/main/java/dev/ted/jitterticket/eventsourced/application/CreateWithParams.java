package dev.ted.jitterticket.eventsourced.application;

@FunctionalInterface
public interface CreateWithParams<AGGREGATE, PARAMS> {
    AGGREGATE execute(PARAMS params);
}
