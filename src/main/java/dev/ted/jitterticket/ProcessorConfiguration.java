package dev.ted.jitterticket;

import dev.ted.jitterticket.eventsourced.application.Checkpoint;
import dev.ted.jitterticket.eventsourced.application.CommandExecutorFactory;
import dev.ted.jitterticket.eventsourced.application.ConcertStartedProcessor;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

@Configuration
public class ProcessorConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProcessorConfiguration.class);

    @Bean
    public ConcertStartedProcessor concertStartedProcessor(
            EventStore<ConcertId, ConcertEvent, Concert> concertStore
    ) {
        ConcertStartedProcessor concertStartedProcessor =
                ConcertStartedProcessor.create(
                        ForkJoinPool.commonPool(),
                        Clock.systemDefaultZone(),
                        CommandExecutorFactory.create(concertStore));
        log.info("Concert Started Processor: catching up on all events");
        Stream<ConcertEvent> catchUpEventStream =
                concertStore.allEventsAfter(Checkpoint.INITIAL);
        log.info("Handling the events...");
        concertStartedProcessor.handle(catchUpEventStream);
        concertStore.subscribe(concertStartedProcessor);
        return concertStartedProcessor;
    }
}
