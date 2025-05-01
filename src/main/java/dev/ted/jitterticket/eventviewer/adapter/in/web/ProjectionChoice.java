package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.List;
import java.util.UUID;

public abstract class ProjectionChoice {

    protected final String aggregateName;
    protected final String urlPathVariable;

    public ProjectionChoice(String aggregateName, String urlPathVariable) {
        this.aggregateName = aggregateName;
        this.urlPathVariable = urlPathVariable;
    }

    public abstract List<AggregateSummaryView> aggregateSummaryViews();

    public abstract List<? extends Event> eventsFor(UUID uuid);

    public abstract List<String> propertiesOfProjectionFrom(List<? extends Event> events);

    public String aggregateName() {
        return aggregateName;
    }

    public String urlPath() {
        return "/event-viewer/" + urlPathVariable;
    }

}
