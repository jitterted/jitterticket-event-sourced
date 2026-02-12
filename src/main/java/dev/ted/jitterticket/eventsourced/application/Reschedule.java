package dev.ted.jitterticket.eventsourced.application;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record Reschedule(LocalDateTime showDateTime, LocalTime doorsTime) {}
