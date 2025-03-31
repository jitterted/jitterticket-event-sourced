package dev.ted.jitterticket.eventsourced;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class JitterTicketEventSourcedApplicationTests {

	@Test
	void contextLoads() {
	}

}
