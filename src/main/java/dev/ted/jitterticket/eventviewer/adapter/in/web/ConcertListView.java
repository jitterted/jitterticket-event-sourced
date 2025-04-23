package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertSummary;

public record ConcertListView(String concertId, String artist) {
    
    public static ConcertListView from(ConcertSummary concertSummary) {
        return new ConcertListView(
                concertSummary.concertId().id().toString(),
                concertSummary.artist()
        );
    }
}