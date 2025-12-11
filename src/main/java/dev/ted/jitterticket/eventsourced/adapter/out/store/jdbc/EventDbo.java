package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@SuppressWarnings("SpringDataJdbcAssociatedDbElementsInspection")
@Table("events")
public class EventDbo {

    // since the eventSequence is DB-generated, and is therefore unique per-event
    // we no longer need a composite ID, though we still index on the aggregateRootId
    // because we often retrieve rows for a specific aggregate
    private UUID aggregateRootId;
    @Id
    private Long eventSequence;

    private String eventType;
    private String json;

    @ReadOnlyProperty
    private OffsetDateTime createdAt;

    private Integer version;

    public EventDbo() {
    }

    public EventDbo(UUID aggregateRootId,
                    String eventType,
                    String json) {
        this.aggregateRootId = aggregateRootId;
        this.eventType = eventType;
        this.json = json;
        this.version = 1;
    }

    //region Getters and Setters
    public UUID getAggregateRootId() {
        return aggregateRootId;
    }

    public void setAggregateRootId(UUID aggregateRootId) {
        this.aggregateRootId = aggregateRootId;
    }

    public Long getEventSequence() {
        return eventSequence;
    }

    public void setEventSequence(Long eventSequence) {
        this.eventSequence = eventSequence;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
    //endregion getter-setters

    @Override
    public String toString() {
        return "EventDbo: [" +
                "aggregateRootId=" + aggregateRootId +
                ", eventSequence=" + eventSequence +
                ", eventType='" + eventType + '\'' +
                ", json='" + json + '\'' +
                ", createdAt=" + createdAt +
                ", version=" + version +
                ']';
    }
}

