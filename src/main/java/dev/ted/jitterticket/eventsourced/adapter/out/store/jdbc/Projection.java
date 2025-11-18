package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("projections")
public class Projection {
    @Id
    private String projectionName;
    private int version;
    private long lastGlobalEventSequenceSeen;

    public Projection() {
    }

    public Projection(String projectionName,
                      int version,
                      long lastGlobalEventSequenceSeen) {
        this.projectionName = projectionName;
        this.version = version;
        this.lastGlobalEventSequenceSeen = lastGlobalEventSequenceSeen;
    }

    public String getProjectionName() {
        return projectionName;
    }

    public void setProjectionName(String projectionName) {
        this.projectionName = projectionName;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getLastGlobalEventSequenceSeen() {
        return lastGlobalEventSequenceSeen;
    }

    public void setLastGlobalEventSequenceSeen(long lastGlobalEventSequenceSeen) {
        this.lastGlobalEventSequenceSeen = lastGlobalEventSequenceSeen;
    }
}
