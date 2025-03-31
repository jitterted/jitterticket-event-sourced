package dev.ted.jitterticket.eventsourced;

import org.springframework.boot.SpringApplication;

public class TestJitterticketEventSourcedApplication {

	public static void main(String[] args) {
		SpringApplication.from(JitterticketEventSourcedApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
