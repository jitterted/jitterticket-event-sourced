package dev.ted.jitterticket.eventsourced;

import java.util.List;

public class Concert {

    public static Concert schedule() {
        return new Concert();
    }

    public List<ConcertEvent> uncommittedEvents() {
        return null;
    }
}
