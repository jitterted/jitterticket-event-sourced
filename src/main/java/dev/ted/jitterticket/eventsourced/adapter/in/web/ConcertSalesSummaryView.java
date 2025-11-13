package dev.ted.jitterticket.eventsourced.adapter.in.web;

public record ConcertSalesSummaryView(
        String concertId,
        String artist,
        String showDate,
        String ticketsSold,
        String totalSales
) {
}
