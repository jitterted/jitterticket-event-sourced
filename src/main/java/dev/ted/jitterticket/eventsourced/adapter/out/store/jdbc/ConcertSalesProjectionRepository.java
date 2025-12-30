package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import java.util.Optional;

public interface ConcertSalesProjectionRepository extends ListCrudRepository<ConcertSalesProjectionDbo, String> {

    boolean existsByProjectionName(String projectionName);

    @Query("SELECT last_event_sequence_seen FROM concert_sales_projection WHERE projection_name = :projectionName")
    Optional<Long> findLastEventSequenceSeenByProjectionName(String projectionName);
}
