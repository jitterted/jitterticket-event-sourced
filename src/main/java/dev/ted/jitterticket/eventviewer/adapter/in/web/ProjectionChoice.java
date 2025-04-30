package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public record ProjectionChoice(String description, String urlPath,
                               Function<UUID, List<? extends Event>> uuidToAllEvents,
                               Function<List<? extends Event>, List<String>> eventsToStrings) {}
