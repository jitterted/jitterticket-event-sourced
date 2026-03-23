package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryScheduledConcertsProjectionPersistence
        implements ProjectionPersistencePort<ScheduledConcerts, ScheduledConcertsDelta> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private final Map<ConcertId, ScheduledConcert> state = new HashMap<>();

    @Override
    public Snapshot<ScheduledConcerts> loadSnapshot() {
        return new Snapshot<>(new ScheduledConcerts(List.copyOf(state.values())), checkpoint);
    }

    @Override
    public void saveDelta(ScheduledConcertsDelta delta, Checkpoint newCheckpoint) {
        delta.upsertedConcerts().forEach(concert -> state.put(concert.concertId(), concert));
        delta.removedConcertIds().forEach(state::remove);
        checkpoint = newCheckpoint;
    }
}
