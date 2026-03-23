package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.time.LocalDate;

public record ScheduledConcert(ConcertId concertId,
                               LocalDate showDate) {}
