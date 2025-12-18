package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

@Table("projection_metadata")
public class ProjectionMetadata implements Persistable<String> {
    @Id
    private String projectionName;
    private long lastEventSequenceSeen;

    @Transient
    private boolean isNew;

    @PersistenceCreator
    public ProjectionMetadata() {
    }

    public ProjectionMetadata(String projectionName,
                              long lastEventSequenceSeen) {
        this.projectionName = projectionName;
        this.lastEventSequenceSeen = lastEventSequenceSeen;
        this.isNew = true;
    }

    //region Persistable Implementation
    @Override
    public String getId() {
        return projectionName;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    //endregion

    public String getProjectionName() {
        return projectionName;
    }

    public void setProjectionName(String projectionName) {
        this.projectionName = projectionName;
    }

    public long getLastEventSequenceSeen() {
        return lastEventSequenceSeen;
    }

    public void setLastEventSequenceSeen(long lastEventSequenceSeen) {
        this.lastEventSequenceSeen = lastEventSequenceSeen;
    }

}
