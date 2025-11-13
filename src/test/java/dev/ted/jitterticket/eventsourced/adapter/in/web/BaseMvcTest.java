package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.TixConfiguration;
import dev.ted.jitterticket.eventsourced.adapter.TestEventStoreConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

@Tag("mvc")
@Tag("spring")
@Import({
        TestEventStoreConfiguration.class,
        TixConfiguration.class
})
public class BaseMvcTest {
    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("events.directory", () -> tempDir.toString());
    }
}
