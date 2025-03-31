package dev.ted.jitterticket.eventsourced;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ConcertTest {

    @Nested
    class CommandsGenerateEvents {

        @Test
        public void scheduleConcertGeneratesConcertScheduled() {
            Concert concert = Concert.schedule();//price, showDateTime, doorsTime, capacity, maxTicketsPerPurchase);

            List<ConcertEvent> events = concert.uncommittedEvents();

            assertThat(events)
                    .containsExactly(new ConcertScheduled());
        }
        
    }
    
}
