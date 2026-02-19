package dev.ted.jitterticket.eventsourced.application;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record ScheduleParams(String artist,
                             int ticketPrice,
                             LocalDateTime showDateTime,
                             LocalTime doorsTime,
                             int capacity,
                             int maxTicketsPerPurchase) {}
