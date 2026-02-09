package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

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
        concertEventStream.forEach(
                concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled cs -> {
                            LocalDateTime showDateTime = cs.showDateTime();
                            alarmMap.put(concertEvent.concertId(), showDateTime);
                        }

                        case ConcertRescheduled cr -> {
                            LocalDateTime showDateTime = cr.newShowDateTime();
                            alarmMap.put(concertEvent.concertId(), showDateTime);
                        }

                        case TicketSalesStopped ticketSalesStopped -> {
                        }

                        case TicketsSold _ -> {
                            // ignore
                        }
                    }
                });
    }
}
