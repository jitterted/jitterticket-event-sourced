package dev.ted.jitterticket.eventsourced.application;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record RescheduleParams(LocalDateTime showDateTime, LocalTime doorsTime) {}
