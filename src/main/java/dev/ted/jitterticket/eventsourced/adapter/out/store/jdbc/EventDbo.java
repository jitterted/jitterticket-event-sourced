package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Table("events")

public class EventDbo implements Persistable<UUID> {

    // Composite primary key fields: prior to Spring Data JDBC 4.0, can't easily use a composite ID for @Id, but we have to mark one of these as the primary key
    // so we mark the aggregateRootId and let the database handle the composite key constraint
    @Id
    private UUID aggregateRootId;
    private Integer eventSequence;

    private String eventType;
    private String json;

    @ReadOnlyProperty
    private OffsetDateTime createdAt;

    @ReadOnlyProperty
    private Long globalSequence;

    private Integer version;

    @Transient
    private boolean isNew;

    // Constructors
    public EventDbo() {
    }

    public EventDbo(UUID aggregateRootId, Integer eventSequence, String eventType, String json) {
        this.aggregateRootId = aggregateRootId;
        this.eventSequence = eventSequence;
        this.eventType = eventType;
        this.json = json;
        this.version = 1;
        this.isNew = true;
    }

    //region Persistable interface methods
    @Override
    public UUID getId() {
        return aggregateRootId;
    }

    @Override
    public boolean isNew() {
        return isNew || globalSequence == null;
    }

    public void markNotNew() {
        this.isNew = false;
    }
    //endregion Persistable

    //region Getters and Setters
    public UUID getAggregateRootId() {
        return aggregateRootId;
    }

    public void setAggregateRootId(UUID aggregateRootId) {
        this.aggregateRootId = aggregateRootId;
    }

    public Integer getEventSequence() {
        return eventSequence;
    }

    public void setEventSequence(Integer eventSequence) {
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

    public Long getGlobalSequence() {
        return globalSequence;
    }

    public void setGlobalSequence(Long globalSequence) {
        this.globalSequence = globalSequence;
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
                ", globalSequence=" + globalSequence +
                ", version=" + version +
                ']';
    }
}

