package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectionMetadataRepository extends ListCrudRepository<ProjectionMetadata, String> {

    @Query("SELECT last_event_sequence_seen FROM projection_metadata WHERE projection_name = :projectionName")
    Optional<Long> lastGlobalEventSequenceSeenByProjectionName(@Param("projectionName") String projectionName);

    // does not actually return a Long, tries to return a ProjectionMetadata entity
    // Optional<Long> findLastGlobalEventSequenceSeenByProjectionName(String projectionName);

    @Query("INSERT INTO projection_metadata (projection_name, last_event_sequence_seen) VALUES (:projectionName, :lastGlobalEventSequenceSeen) ON CONFLICT (projection_name) DO NOTHING RETURNING last_event_sequence_seen")
    Optional<Long> saveIfNotExist(@Param("projectionName") String projectionName, @Param("lastGlobalEventSequenceSeen") Long lastGlobalEventSequenceSeen);

    boolean existsByProjectionName(String projectionName);
}
