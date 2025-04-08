package dev.ted.jitterticket.eventsourced.adapter.in.web;

public record ConcertView(
        String concertId,
        String artist,
        String ticketPrice,
        String showDate,
        String showTime) {}
