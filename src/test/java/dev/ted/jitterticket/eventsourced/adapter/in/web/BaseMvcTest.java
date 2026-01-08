package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.ProjectionConfiguration;
import dev.ted.jitterticket.UseCaseConfiguration;
import dev.ted.jitterticket.eventsourced.adapter.TestEventStoreConfiguration;
import dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc.ConcertSalesProjectionRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;

@Tag("mvc")
@Import({
        TestEventStoreConfiguration.class,
        ProjectionConfiguration.class,
        UseCaseConfiguration.class
})
public class BaseMvcTest {
    @TempDir
    static Path tempDir;

    @MockitoBean
    ConcertSalesProjectionRepository concertSalesProjectionRepository;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("events.directory", () -> tempDir.toString());
    }
}
