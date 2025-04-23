package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.application.ConcertTicketView;

/**
 * View object for displaying concert list with only ConcertId and Artist.
 */
public record ConcertListView(
        String concertId,
        String artist) {
    
    /**
     * Creates a ConcertListView from a ConcertTicketView.
     */
    public static ConcertListView from(ConcertTicketView concertTicketView) {
        return new ConcertListView(
                concertTicketView.concertId().id().toString(),
                concertTicketView.artist()
        );
    }
}