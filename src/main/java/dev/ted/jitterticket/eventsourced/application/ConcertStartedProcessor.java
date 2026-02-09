package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class ConcertStartedProcessor implements EventConsumer<ConcertEvent> {

    private final Map<ConcertId, LocalDateTime> alarmMap = new HashMap<>();

    // internally we probably store this as Map<ConcertId, ConcertAlarm>
    // where ConcertAlarm is (LocalDateTime showDateTime, ScheduledFuture<?>)
    public Map<ConcertId, LocalDateTime> alarms() {
        return alarmMap;
    }

    @Override
    public void handle(Stream<ConcertEvent> concertEventStream) {
        if (concertEventStream.toList().getFirst()
                instanceof ConcertScheduled cs) {
            alarmMap.put(cs.concertId(), cs.showDateTime());
        }
    }
}
