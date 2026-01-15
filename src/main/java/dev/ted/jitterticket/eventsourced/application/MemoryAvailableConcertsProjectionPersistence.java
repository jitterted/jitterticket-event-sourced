package dev.ted.jitterticket.eventsourced.application;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryAvailableConcertsProjectionPersistence implements ProjectionPersistencePort<AvailableConcerts, AvailableConcertsDelta> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private final Map<dev.ted.jitterticket.eventsourced.domain.concert.ConcertId, AvailableConcert> state = new HashMap<>();

    @Override
    public Snapshot<AvailableConcerts> loadSnapshot() {
        return new Snapshot<>(new AvailableConcerts(List.copyOf(state.values())), checkpoint);
    }

    @Override
    public void saveDelta(AvailableConcertsDelta delta, Checkpoint newCheckpoint) {
        delta.upsertedConcerts().forEach(concert -> state.put(concert.concertId(), concert));
        delta.removedConcertIds().forEach(state::remove);
        checkpoint = newCheckpoint;
    }
}
