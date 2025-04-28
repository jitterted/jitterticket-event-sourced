package dev.ted.jitterticket.eventsourced.adapter.out.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.UUID;

public class EventDto<EVENT extends Event> {
    private final UUID aggRootId; // ID for the Aggregate Root
    private final long eventSequence;
    private final String eventType;
    private final String json; // blob of data - schemaless

    /*
        Table schema:

        PK   AggRootId
             EventSequence (monotonically increasing per AggRootId)
             Sequence?? (this might be a globally ordered sequence from the DB?)
             Version (for versioning the schema)
        JSON String eventContent

        AggRootId | EventSequence | Sequence?? | Version | Timestamp | EventType          |  JSON Content
        ----------------------------------------------------------------------------------------------------------------
        0         | 0             |            |         |           | ConcertScheduled   | {id: 0, artist: "Judy", ... }
        1         | 0             |            |         |           | ConcertScheduled   | {id: 1, artist: "Betty", ... }
        0         | 1             |            |         |           | TicketsSold        | {id: 0, quantity: 4, totalPaid: 120 }
        0         | 2             |            |         |           | ConcertRescheduled | {id: 0, newShowDateTime: 2025-11-11 11:11, newDoorsTime: 10:11 }
    */


    // -- the following mapper and maps should be externalized to some configuration
    //    so that when adding (and especially renaming) classes, the mapping works
//    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventDto(UUID aggRootId, Long eventSequence, String eventClassName, String json) {
        if (eventClassName == null) {
            throw new IllegalArgumentException("Event class name cannot be null, JSON is: " + json);
        }
        this.aggRootId = aggRootId;
        this.eventSequence = eventSequence;
        this.eventType = eventClassName;
        this.json = json;
    }

    public static <EVENT extends Event> EventDto<EVENT> from(UUID aggRootId, Long eventSequence, EVENT event) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            String json = objectMapper.writeValueAsString(event);
            String fullyQualifiedClassName = event.getClass().getName();
            return new EventDto<>(aggRootId, eventSequence, fullyQualifiedClassName, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public EVENT toDomain() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            Class<EVENT> valueType = (Class<EVENT>) Class.forName(eventType);
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Problem converting JSON: " + json + " to " + eventType, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
