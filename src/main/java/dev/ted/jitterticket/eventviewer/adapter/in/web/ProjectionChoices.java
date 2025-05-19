package dev.ted.jitterticket.eventviewer.adapter.in.web;

import java.util.Collection;
import java.util.Map;

public class ProjectionChoices {
    protected Map<String, ProjectionChoice> projectionChoices;

    public ProjectionChoices(Map<String, ProjectionChoice> projectionChoices) {
        this.projectionChoices = projectionChoices;
    }

    public Collection<ProjectionChoice> choices() {
        return projectionChoices.values();
    }

    public ProjectionChoice choiceFor(String aggregateName) {
        return projectionChoices.get(aggregateName);
    }
}