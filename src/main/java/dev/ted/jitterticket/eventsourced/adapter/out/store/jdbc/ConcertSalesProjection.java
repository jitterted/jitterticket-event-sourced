package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Table("concert_sales_projection")
public class ConcertSalesProjection {

    @Id
    private UUID concertId;
    private String artistName;
    private LocalDate concertDate;
    private int ticketsSold;
    private int totalSales;

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
    }

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
}
