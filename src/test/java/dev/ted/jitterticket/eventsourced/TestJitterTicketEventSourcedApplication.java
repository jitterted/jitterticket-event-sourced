package dev.ted.jitterticket.eventsourced;

import dev.ted.jitterticket.JitterTicketEventSourcedApplication;
import org.springframework.boot.SpringApplication;

public class TestJitterTicketEventSourcedApplication {

	public static void main(String[] args) {
		SpringApplication.from(JitterTicketEventSourcedApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
