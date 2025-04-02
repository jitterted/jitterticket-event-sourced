package dev.ted.jitterticket.eventsourced.adapter.out.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.UUID;

public class EventDto {
    private final UUID aggRootId; // ID for the Aggregate Root
    private final int eventId;
    private final String eventType;
    private final String json;

    // -- the following mapper and maps should be externalized to some configuration
    //    so that when adding (and especially renaming) classes, the mapping works
//    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventDto(UUID aggRootId, int eventId, String eventClassName, String json) {
        if (eventClassName == null) {
            throw new IllegalArgumentException("Event class name cannot be null, JSON is: " + json);
        }
        this.aggRootId = aggRootId;
        this.eventId = eventId;
        this.eventType = eventClassName;
        this.json = json;
    }

    public static EventDto from(UUID aggRootId, int eventId, Event event) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            String json = objectMapper.writeValueAsString(event);
            String fullyQualifiedClassName = event.getClass().getName();
            if (fullyQualifiedClassName == null) {
                throw new IllegalArgumentException("Unknown event class: " + event.getClass().getSimpleName());
            }
            return new EventDto(aggRootId, eventId, fullyQualifiedClassName, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public Event toDomain() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            Class<? extends Event> valueType = (Class<? extends Event>) Class.forName(eventType);
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Problem converting JSON: " + json + " to " + eventType, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
