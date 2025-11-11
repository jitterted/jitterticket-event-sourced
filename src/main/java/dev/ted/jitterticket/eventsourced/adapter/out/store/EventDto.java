package dev.ted.jitterticket.eventsourced.adapter.out.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.TicketOrderId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertRescheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertScheduled;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public class EventDto<EVENT extends Event> {
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();
    private final UUID aggregateRootId; // ID for the Aggregate Root
    private final Integer eventSequence;
    private final String eventType;
    private final String json; // blob of data - schemaless

    /*
        Table schema:

        PK   AggregateRootId
             EventSequence (monotonically increasing per AggRootId)
             Sequence?? (this might be a globally ordered sequence from the DB?)
             Version (for versioning the schema)
        JSON String eventContent

        AggRootId | EventSequence | Sequence?? | Version | Timestamp | EventType          |  JSON Content
        ----------------------------------------------------------------------------------------------------------------
        0         | 0             |            |         |           | ConcertScheduled   | {id: 0, eventSequence: 0, artist: "Judy", ... }
        1         | 0             |            |         |           | ConcertScheduled   | {id: 1, eventSequence: 0, artist: "Betty", ... }
        0         | 1             |            |         |           | TicketsSold        | {id: 0, eventSequence: 1, quantity: 4, totalPaid: 120 }
        0         | 2             |            |         |           | ConcertRescheduled | {id: 0, eventSequence: 2, newShowDateTime: 2025-11-11 11:11, newDoorsTime: 10:11 }
    */


    // -- the following mapper and maps should be externalized to some configuration
    //    so that when adding (and especially renaming) classes, the mapping works
//    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventDto(UUID aggregateRootId, Integer eventSequence, String eventClassName, String json) {
        if (eventClassName == null) {
            throw new IllegalArgumentException("Event class name cannot be null, JSON is: " + json);
        }
        this.aggregateRootId = aggregateRootId;
        this.eventSequence = eventSequence;
        this.eventType = eventClassName;
        this.json = json;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        objectMapper.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature());

        objectMapper.registerModule(new JavaTimeModule());

        objectMapper.addMixIn(Event.class, EventMixin.class);
        objectMapper.addMixIn(CustomerEvent.class, CustomerEventMixin.class);
        objectMapper.addMixIn(ConcertEvent.class, ConcertEventMixin.class);
        objectMapper.addMixIn(CustomerRegistered.class, CustomerRegisteredMixin.class);
        objectMapper.addMixIn(ConcertScheduled.class, ConcertScheduledMixin.class);
        objectMapper.addMixIn(ConcertRescheduled.class, ConcertRescheduledMixin.class);
        objectMapper.addMixIn(TicketsSold.class, TicketsSoldMixin.class);
        objectMapper.addMixIn(TicketsPurchased.class, TicketsPurchasedMixin.class);

        return objectMapper;
    }

    public static <EVENT extends Event> EventDto<EVENT> from(UUID aggregateRootId, Integer eventSequence, EVENT event) {

        try {
            String json = OBJECT_MAPPER.writeValueAsString(event);
            String fullyQualifiedClassName = event.getClass().getName();
            return new EventDto<>(aggregateRootId, eventSequence, fullyQualifiedClassName, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public EVENT toDomain() {

        try {
            Class<EVENT> valueType = (Class<EVENT>) Class.forName(eventType);
            return OBJECT_MAPPER.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Problem converting JSON: " + json + " to " + eventType, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getAggregateRootId() {
        return aggregateRootId;
    }

    public Integer getEventSequence() {
        return eventSequence;
    }

    public String getEventType() {
        return eventType;
    }

    public String getJson() {
        return json;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventDto<?> eventDto = (EventDto<?>) o;
        return aggregateRootId.equals(eventDto.aggregateRootId) && eventSequence.equals(eventDto.eventSequence) && eventType.equals(eventDto.eventType) && json.equals(eventDto.json);
    }

    @Override
    public int hashCode() {
        int result = aggregateRootId.hashCode();
        result = 31 * result + eventSequence.hashCode();
        result = 31 * result + eventType.hashCode();
        result = 31 * result + json.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "EventDto {" +
                "aggregateRootId=" + aggregateRootId +
                ", eventSequence=" + eventSequence +
                ", eventType='" + eventType + '\'' +
                ", json='" + json + '\'' +
                '}';
    }
}

abstract class EventMixin {
    @JsonProperty("eventSequence")
    public abstract Integer eventSequence();
}

abstract class CustomerEventMixin {
    @JsonProperty("customerId")
    public abstract CustomerId customerId();
}

abstract class ConcertEventMixin {
    @JsonProperty("concertId")
    public abstract ConcertId concertId();
}

abstract class CustomerRegisteredMixin {
    @JsonCreator
    public CustomerRegisteredMixin(
            @JsonProperty("customerId") CustomerId customerId,
            @JsonProperty("eventSequence") Integer eventSequence,
            @JsonProperty("customerName") String customerName,
            @JsonProperty("email") String email) {
    }

    @JsonProperty("customerName")
    public abstract String customerName();

    @JsonProperty("email")
    public abstract String email();
}

abstract class ConcertRescheduledMixin {
    @JsonCreator
    public ConcertRescheduledMixin(
            @JsonProperty("concertId") ConcertId concertId,
            @JsonProperty("eventSequence") Integer eventSequence,
            @JsonProperty("newShowDateTime") LocalDateTime newShowDateTime,
            @JsonProperty("newDoorsTime") LocalTime newDoorsTime
    ) {
    }

    @JsonProperty("concertId")
    public abstract ConcertId concertId();

    @JsonProperty("newShowDateTime")
    public abstract LocalDateTime newShowDateTime();

    @JsonProperty("newDoorsTime")
    public abstract LocalTime newDoorsTime();
}

abstract class TicketsSoldMixin {
    @JsonCreator
    public TicketsSoldMixin(
            @JsonProperty("concertId") ConcertId concertId,
            @JsonProperty("eventSequence") Integer eventSequence,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("totalPaid") int totalPaid
    ) {
    }

    @JsonProperty("concertId")
    public abstract ConcertId concertId();

    @JsonProperty("quantity")
    public abstract int quantity();

    @JsonProperty("totalPaid")
    public abstract int totalPaid();
}

abstract class TicketsPurchasedMixin {
    @JsonCreator
    public TicketsPurchasedMixin(
            @JsonProperty("customerId") CustomerId customerId,
            @JsonProperty("eventSequence") Integer eventSequence,
            @JsonProperty("ticketOrderId") TicketOrderId ticketOrderId,
            @JsonProperty("concertId") ConcertId concertId,
            @JsonProperty("quantity") int quantity,
            @JsonProperty("paidAmount") int paidAmount
    ) {
    }

    @JsonProperty("ticketOrderId")
    public abstract TicketOrderId ticketOrderId();

    @JsonProperty("concertId")
    public abstract ConcertId concertId();

    @JsonProperty("quantity")
    public abstract int quantity();

    @JsonProperty("paidAmount")
    public abstract int paidAmount();
}

abstract class ConcertScheduledMixin {
    @JsonCreator
    public ConcertScheduledMixin(
            @JsonProperty("concertId") ConcertId concertId,
            @JsonProperty("eventSequence") Integer eventSequence,
            @JsonProperty("artist") String artist,
            @JsonProperty("ticketPrice") int ticketPrice,
            @JsonProperty("showDateTime") java.time.LocalDateTime showDateTime,
            @JsonProperty("doorsTime") java.time.LocalTime doorsTime,
            @JsonProperty("capacity") int capacity,
            @JsonProperty("maxTicketsPerPurchase") int maxTicketsPerPurchase
    ) {
    }

    @JsonProperty("concertId")
    public abstract ConcertId concertId();

    @JsonProperty("artist")
    public abstract String artist();

    @JsonProperty("ticketPrice")
    public abstract int ticketPrice();

    @JsonProperty("showDateTime")
    public abstract java.time.LocalDateTime showDateTime();

    @JsonProperty("doorsTime")
    public abstract java.time.LocalTime doorsTime();

    @JsonProperty("capacity")
    public abstract int capacity();

    @JsonProperty("maxTicketsPerPurchase")
    public abstract int maxTicketsPerPurchase();
}