package dev.ted.jitterticket.eventsourced;

import dev.ted.jitterticket.eventviewer.adapter.in.web.ProjectionChoices;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Tag("spring")
class JitterTicketEventSourcedApplicationTests {

	@Autowired
	ProjectionChoices projectionChoices;

	@Test
	void customerAndConcertEventStoresAreInjectedProperlyIntoProjectionChoicesInConfiguration() {
		assertThat(projectionChoices.choices())
				.hasSize(2);
		assertThat(projectionChoices.choiceFor("concerts").aggregateSummaryViews())
				.hasSize(10);
		assertThat(projectionChoices.choiceFor("customers").aggregateSummaryViews())
				.hasSize(2);
	}

}
