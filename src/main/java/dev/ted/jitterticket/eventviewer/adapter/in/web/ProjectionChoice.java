package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.util.List;
import java.util.UUID;

public abstract class ProjectionChoice {

    protected final String aggregateName;
    protected final String urlPath;
    protected final String description;

    public ProjectionChoice(String aggregateName, String urlPath, String description) {
        this.aggregateName = aggregateName;
        this.urlPath = urlPath;
        this.description = description;
    }

    public abstract List<AggregateSummaryView> aggregateSummaryViews();

    public abstract List<? extends Event> eventsFor(UUID uuid);

    public abstract List<String> propertiesOfAggregateFrom(List<? extends Event> events);

    public String aggregateName() {
        return aggregateName;
    }

    public String urlPath() {
        return urlPath;
    }

    public String description() {
        return description;
    }
}
