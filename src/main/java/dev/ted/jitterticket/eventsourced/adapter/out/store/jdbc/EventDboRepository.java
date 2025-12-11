package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventDboRepository extends CrudRepository<EventDbo, UUID> {

    /**
     * Find all events for a specific aggregate root in sequence order.
     * This is the primary method for reconstructing the aggregate's state.
     */
    @Query("SELECT * FROM events WHERE aggregate_root_id = :aggregateRootId ORDER BY global_sequence")
    List<EventDbo> findByAggregateRootId(@Param("aggregateRootId") UUID aggregateRootId);

    /**
     * Find all events in global sequence order.
     * Useful for event streaming and projections.
     */
    @Query("SELECT * FROM events ORDER BY global_sequence")
    List<EventDbo> findAllByGlobalSequence();

    /**
     * Find events after a specific global sequence.
     * Useful for catching up event projections or subscribers.
     */
    @Query("SELECT * FROM events WHERE global_sequence > :afterSequence ORDER BY global_sequence")
    List<EventDbo> findEventsAfter(@Param("afterSequence") Long afterSequence);

    /**
     * Find events by type across all aggregates.
     * Useful for building specific event type projections.
     */
    @Query("SELECT * FROM events WHERE event_type = :eventType ORDER BY global_sequence")
    List<EventDbo> findByEventType(@Param("eventType") String eventType);

    /**
     * Get the maximum event sequence for an aggregate.
     * Useful for optimistic locking or determining the next sequence number.
     */
    @Query("SELECT COALESCE(MAX(global_sequence), 0) FROM events WHERE aggregate_root_id = :aggregateRootId")
    Integer getMaxEventSequence(@Param("aggregateRootId") UUID aggregateRootId);

    Optional<EventDbo> findByAggregateRootIdAndEventSequence(UUID aggregateRootId, Integer eventSequence);
}