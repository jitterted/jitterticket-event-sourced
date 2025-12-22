package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class ConcertSalesProjectionRepositoryTest extends DataJdbcContainerTest {

    @Autowired
    private ConcertSalesProjectionRepository concertSalesProjectionRepository;

    @Test
    void savedProjectionExists() {
        ConcertSalesProjectionDbo projectionDbo =
                new ConcertSalesProjectionDbo("projection_name", 0L);

        concertSalesProjectionRepository.save(projectionDbo);

        assertThat(concertSalesProjectionRepository
                           .existsByProjectionName("projection_name"))
                .isTrue();
    }

    @Test
    void savedProjectionUpdatesSetOfConcertSales() {
        ConcertSalesProjectionDbo projectionDbo =
                new ConcertSalesProjectionDbo("projection_name", 2L);
        ConcertSalesDbo concertSalesDbo = new ConcertSalesDbo(
                UUID.randomUUID(), "artist name", LocalDate.now(), 0, 0);
        projectionDbo.setConcertSales(Set.of(concertSalesDbo));

        concertSalesProjectionRepository.save(projectionDbo);

        Optional<ConcertSalesProjectionDbo> dboOptional = concertSalesProjectionRepository.findById("projection_name");
        assertThat(dboOptional)
                .isPresent()
                .get()
                .extracting(ConcertSalesProjectionDbo::getConcertSales, InstanceOfAssertFactories.SET)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("isNew")
                .containsExactly(concertSalesDbo);
    }
}