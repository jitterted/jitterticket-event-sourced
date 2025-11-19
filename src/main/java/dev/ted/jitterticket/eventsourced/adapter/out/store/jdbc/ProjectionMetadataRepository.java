package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.repository.ListCrudRepository;

public interface ProjectionMetadataRepository extends ListCrudRepository<ProjectionMetadata, String> {
}
