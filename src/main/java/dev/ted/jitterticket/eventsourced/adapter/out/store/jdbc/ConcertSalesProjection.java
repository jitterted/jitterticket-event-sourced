package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjector;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.StringJoiner;
import java.util.UUID;

@Table("concert_sales_projection")
public class ConcertSalesProjection implements Persistable<UUID> {

    @Id
    private UUID concertId;
    private String artistName;
    private LocalDate concertDate;
    private int ticketsSold;
    private int totalSales;

    @Transient
    private boolean isNew;

    @PersistenceCreator
    public ConcertSalesProjection() {
    }

    public ConcertSalesProjection(UUID concertId,
                                  String artistName,
                                  LocalDate concertDate,
                                  int ticketsSold,
                                  int totalSales) {
        this.concertId = concertId;
        this.artistName = artistName;
        this.concertDate = concertDate;
        this.ticketsSold = ticketsSold;
        this.totalSales = totalSales;
        this.isNew = true;
    }

    public static ConcertSalesProjection createFromSummary(ConcertSalesProjector.ConcertSalesSummary css) {
        return new ConcertSalesProjection(
                css.concertId().id(),
                css.artist(),
                css.showDateTime().toLocalDate(),
                css.totalQuantity(),
                css.totalSales()
        );
    }

    public ConcertSalesProjector.ConcertSalesSummary toSummary() {
        return new ConcertSalesProjector.ConcertSalesSummary(
                new ConcertId(getConcertId()),
                getArtistName(),
                getConcertDate().atStartOfDay(),
                getTicketsSold(),
                getTotalSales()
        );
    }

    //region Persistable Implementation
    @Override
    public UUID getId() {
        return concertId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }
    //endregion

    public UUID getConcertId() {
        return concertId;
    }

    public void setConcertId(UUID concertId) {
        this.concertId = concertId;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public LocalDate getConcertDate() {
        return concertDate;
    }

    public void setConcertDate(LocalDate concertDate) {
        this.concertDate = concertDate;
    }

    public int getTicketsSold() {
        return ticketsSold;
    }

    public void setTicketsSold(int ticketsSold) {
        this.ticketsSold = ticketsSold;
    }

    public int getTotalSales() {
        return totalSales;
    }

    public void setTotalSales(int totalSales) {
        this.totalSales = totalSales;
    }


    @Override
    public String toString() {
        return new StringJoiner(", ", ConcertSalesProjection.class.getSimpleName() + "[", "]")
                .add("concertId=" + concertId)
                .add("artistName='" + artistName + "'")
                .add("concertDate=" + concertDate)
                .add("ticketsSold=" + ticketsSold)
                .add("totalSales=" + totalSales)
                .add("isNew=" + isNew)
                .toString();
    }
}
