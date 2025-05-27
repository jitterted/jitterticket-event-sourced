package dev.ted.jitterticket.eventsourced;

import dev.ted.jitterticket.eventviewer.adapter.in.web.ProjectionChoices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Tag("spring")
class JitterTicketEventSourcedApplicationTests {

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("events.directory", () -> tempDir.toString());
	}

	@Autowired
	ProjectionChoices projectionChoices;

	@Test
	void customerAndConcertEventStoresAreInjectedProperlyIntoProjectionChoicesInConfiguration() {
		assertThat(projectionChoices.choices())
				.as("Should be two choices of projections: Concerts and Customers")
				.hasSize(2);
		assertThat(projectionChoices.choiceFor("customers").aggregateSummaryViews())
				.as("Should be two sample customers from the TixConfiguration")
				.hasSize(2);
		assertThat(projectionChoices.choiceFor("concerts").aggregateSummaryViews())
				.as("Should be 10 sample concerts from the TixConfiguration")
				.hasSize(10);
	}

}
