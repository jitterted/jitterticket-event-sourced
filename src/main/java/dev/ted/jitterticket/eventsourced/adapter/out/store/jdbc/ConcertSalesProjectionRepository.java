package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.repository.ListCrudRepository;

import java.util.UUID;

public interface ConcertSalesProjectionRepository extends ListCrudRepository<ConcertSalesProjection, UUID> {
}
