package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProjectionMetadataRepository extends ListCrudRepository<ProjectionMetadata, String> {

    @Query("SELECT last_global_event_sequence_seen FROM projection_metadata WHERE projection_name = :projectionName")
    Optional<Long> lastGlobalSequenceSeenByProjectionName(@Param("projectionName") String projectionName);

}
