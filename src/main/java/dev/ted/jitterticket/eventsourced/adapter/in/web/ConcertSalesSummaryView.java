package dev.ted.jitterticket.eventsourced.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSalesProjector;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public record ConcertSalesSummaryView(
        String concertId,
        String artist,
        String showDate,
        String ticketsSold,
        String totalSales
) {
    static ConcertSalesSummaryView from(ConcertSalesProjector.ConcertSalesSummary concertSalesSummary) {
        return new ConcertSalesSummaryView(
                concertSalesSummary.concertId().id().toString(),
                concertSalesSummary.artist(),
                concertSalesSummary.showDateTime()
                                   .toLocalDate()
                                   .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)),
                String.valueOf(concertSalesSummary.totalQuantity()),
                "$" + concertSalesSummary.totalSales()
        );
    }
}
