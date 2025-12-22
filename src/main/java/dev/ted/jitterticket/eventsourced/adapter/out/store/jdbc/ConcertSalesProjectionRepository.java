package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.repository.ListCrudRepository;

public interface ConcertSalesProjectionRepository extends ListCrudRepository<ConcertSalesProjectionDbo, String> {

    boolean existsByProjectionName(String projectionName);

}
