package dev.ted.jitterticket.eventsourced.application;

import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ConcertTicketView(ConcertId concertId,
                                String artist,
                                int ticketPrice,
                                LocalDateTime showDateTime,
                                LocalTime doorsTime) {}
