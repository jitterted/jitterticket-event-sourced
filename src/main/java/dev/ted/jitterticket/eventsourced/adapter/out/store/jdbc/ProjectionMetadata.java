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
    private long lastGlobalEventSequenceSeen;

    @Transient
    private boolean isNew;

    @PersistenceCreator
    public ProjectionMetadata() {
    }

    public ProjectionMetadata(String projectionName,
                              long lastGlobalEventSequenceSeen) {
        this.projectionName = projectionName;
        this.lastGlobalEventSequenceSeen = lastGlobalEventSequenceSeen;
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

    public long getLastGlobalEventSequenceSeen() {
        return lastGlobalEventSequenceSeen;
    }

    public void setLastGlobalEventSequenceSeen(long lastGlobalEventSequenceSeen) {
        this.lastGlobalEventSequenceSeen = lastGlobalEventSequenceSeen;
    }

}
