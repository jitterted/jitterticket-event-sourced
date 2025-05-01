package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummary;

public record AggregateSummaryView(String concertId, String artist) {
    
    public static AggregateSummaryView of(ConcertSummary concertSummary) {
        return new AggregateSummaryView(
                concertSummary.concertId().id().toString(),
                concertSummary.artist()
        );
    }
}