package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.repository.ListCrudRepository;

public interface ProjectionRepository extends ListCrudRepository<Projection, String> {
}
