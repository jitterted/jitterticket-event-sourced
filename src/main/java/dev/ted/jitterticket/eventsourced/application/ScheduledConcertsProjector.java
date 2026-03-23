package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ScheduledConcertsProjector implements
        DomainProjector<ConcertEvent, ScheduledConcerts, ScheduledConcertsDelta> {


    @Override
    public ProjectorResult<ScheduledConcerts, ScheduledConcertsDelta>
    project(ScheduledConcerts currentState,
            Stream<ConcertEvent> concertEventStream) {

        ProjectorMaps projectorMaps = ProjectorMaps.from(currentState);

        concertEventStream.forEach(
                concertEvent -> {
                    switch (concertEvent) {
                        case ConcertScheduled scheduled -> {
                            projectorMaps.put(scheduled.concertId(),
                                              scheduled.showDateTime()
                            );
                        }
                        case ConcertRescheduled rescheduled -> {
                            projectorMaps.put(rescheduled.concertId(),
                                              rescheduled.newShowDateTime()
                            );
                        }
                        case TicketSalesStopped _, TicketsSold _ -> {
                        }
                    }
                }
        );

        return projectorMaps.result();
    }

    static class ProjectorMaps {
        private static final List<ConcertId> ALWAYS_EMPTY_REMOVED_CONCERT_IDS = Collections.emptyList();
        private final Map<ConcertId, ScheduledConcert> scheduledConcertsMap = new HashMap<>();
        private final Map<ConcertId, ScheduledConcert> upsertedConcertsMap = new HashMap<>();

        static ProjectorMaps from(ScheduledConcerts currentState) {
            ProjectorMaps projectorMaps = new ProjectorMaps();
            projectorMaps.loadMapFrom(currentState);
            return projectorMaps;
        }

        void put(ConcertId concertId, LocalDateTime showDateTime) {
            ScheduledConcert scheduledConcert = new ScheduledConcert(
                    concertId, showDateTime.toLocalDate());
            scheduledConcertsMap.put(concertId, scheduledConcert);
            upsertedConcertsMap.put(concertId, scheduledConcert);
        }

        void loadMapFrom(ScheduledConcerts currentState) {
            currentState.scheduledConcerts()
                        .forEach(concert -> scheduledConcertsMap.put(concert.concertId(), concert));
        }

        ProjectorResult<ScheduledConcerts, ScheduledConcertsDelta> result() {
            return new ProjectorResult<>(
                    new ScheduledConcerts(List.copyOf(scheduledConcertsMap.values())),
                    new ScheduledConcertsDelta(
                            List.copyOf(upsertedConcertsMap.values()),
                            ProjectorMaps.ALWAYS_EMPTY_REMOVED_CONCERT_IDS
                    ));
        }
    }
}
