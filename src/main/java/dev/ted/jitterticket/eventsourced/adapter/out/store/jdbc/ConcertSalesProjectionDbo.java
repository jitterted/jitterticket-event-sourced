package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Set;

@Table("concert_sales_projection")
public class ConcertSalesProjectionDbo implements Persistable<String> {
    @Id
    private String projectionName;
    private long lastEventSequenceSeen;
    @MappedCollection(idColumn = "concert_sales_projection")
    private Set<ConcertSalesDbo> concertSales = Set.of();

    @Transient
    private boolean isNew;

    @PersistenceCreator
    public ConcertSalesProjectionDbo() {
    }

    public ConcertSalesProjectionDbo(String projectionName,
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

    public Set<ConcertSalesDbo> getConcertSales() {
        return concertSales;
    }

    public void setConcertSales(Set<ConcertSalesDbo> concertSales) {
        this.concertSales = concertSales;
    }
}
