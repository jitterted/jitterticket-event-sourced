package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MemoryAvailableConcertsProjectionPersistence
        implements ProjectionPersistencePort<AvailableConcerts, AvailableConcertsDelta> {

    private Checkpoint checkpoint = Checkpoint.INITIAL;
    private final Map<ConcertId, AvailableConcert> state = new HashMap<>();

    @Override
    public Checkpointed<AvailableConcerts> loadSnapshot() {
        return new Checkpointed<>(new AvailableConcerts(List.copyOf(state.values())), checkpoint);
    }

    @Override
    public NewDomainProjector<AvailableConcerts, AvailableConcertsDelta> loadProjector() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void saveDelta(Checkpointed<AvailableConcertsDelta> checkpointed) {
        checkpointed.state().upsertedConcerts().forEach(concert -> state.put(concert.concertId(), concert));
        checkpointed.state().removedConcertIds().forEach(state::remove);
        this.checkpoint = checkpointed.checkpoint();
    }
}
