package dev.ted.jitterticket.eventsourced.adapter.out.store;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import dev.ted.jitterticket.eventsourced.domain.concert.TicketSalesStopped;
import dev.ted.jitterticket.eventsourced.domain.concert.TicketsSold;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerRegistered;
import dev.ted.jitterticket.eventsourced.domain.customer.TicketsPurchased;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;

/**
 *  Represents an Event as JSON with some metadata for converting back to concrete Event object
 */
public class EventDto<EVENT extends Event> {
    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private final UUID aggregateRootId; // ID for the Aggregate Root
    private final Long eventSequence;
    private final String eventType;
    private final String json; // blob of data - schemaless

    /*
        Table schema:

        PK   AggregateRootId
             EventSequence (a globally ordered sequence from the DB, starts at 1 for PostgreSQL)
             Version (optional: for versioning the schema)
        JSON String eventContent

        AggRootId | EventSequence | Version | Timestamp | EventType          |  JSON Content
        ------------------------------------------------------------------------------------------
        0         | 1             |         |           | ConcertScheduled   | {id: 0, eventSequence: 0, artist: "Judy", ... }
        1         | 2             |         |           | ConcertScheduled   | {id: 1, eventSequence: 0, artist: "Betty", ... }
        0         | 3             |         |           | TicketsSold        | {id: 0, eventSequence: 1, quantity: 4, totalPaid: 120 }
        0         | 4             |         |           | ConcertRescheduled | {id: 0, eventSequence: 2, newShowDateTime: 2025-11-11 11:11, newDoorsTime: 10:11 }
    */


    // -- the following mapper and maps should be externalized to some configuration
    //    so that when adding (and especially renaming) classes, the mapping works

    public EventDto(UUID aggregateRootId,
                    Long eventSequence,
                    String eventClassName,
                    String json) {
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
        objectMapper.addMixIn(TicketSalesStopped.class, TicketSalesStoppedMixin.class);

        return objectMapper;
    }

    public static <EVENT extends Event> EventDto<EVENT> from(
            UUID aggregateRootId,
            Long eventSequence,
            EVENT event) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(event);
            String fullyQualifiedClassName = event.getClass().getName();
            return new EventDto<>(aggregateRootId,
                                  eventSequence,
                                  fullyQualifiedClassName,
                                  json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public EVENT toDomain() {

        try {
            Class<EVENT> valueType = (Class<EVENT>) Class.forName(eventType);
            EVENT event = OBJECT_MAPPER.readValue(json, valueType);
            event.setEventSequence(this.eventSequence);
            return event;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Problem converting JSON: " + json + " to " + eventType, e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public UUID getAggregateRootId() {
        return aggregateRootId;
    }

    public Long getEventSequence() {
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
        return aggregateRootId.equals(eventDto.aggregateRootId) && Objects.equals(eventSequence, eventDto.eventSequence) && eventType.equals(eventDto.eventType) && json.equals(eventDto.json);
    }

    @Override
    public int hashCode() {
        int result = aggregateRootId.hashCode();
        result = 31 * result + Objects.hashCode(eventSequence);
        result = 31 * result + eventType.hashCode();
        result = 31 * result + json.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EventDto.class.getSimpleName() + "[", "]")
                .add("aggregateRootId=" + aggregateRootId)
                .add("globalEventSequence=" + eventSequence)
                .add("eventType='" + eventType + "'")
                .add("json='" + json + "'")
                .toString();
    }

}

abstract class EventMixin {
    @JsonIgnore
    public abstract Long eventSequence();
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
            @JsonProperty("eventSequence") Long eventSequence,
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
            @JsonProperty("eventSequence") Long eventSequence,
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
            @JsonProperty("eventSequence") Long eventSequence,
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
            @JsonProperty("eventSequence") Long eventSequence,
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
            @JsonProperty("eventSequence") Long eventSequence,
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

abstract class TicketSalesStoppedMixin {
    @JsonCreator
    public TicketSalesStoppedMixin(
            @JsonProperty("concertId") ConcertId concertId,
            @JsonProperty("eventSequence") Long eventSequence
    ) {
    }

    @JsonProperty("concertId")
    public abstract ConcertId concertId();
}
