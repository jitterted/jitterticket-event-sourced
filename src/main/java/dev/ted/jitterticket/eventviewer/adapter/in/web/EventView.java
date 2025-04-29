package dev.ted.jitterticket.eventviewer.adapter.in.web;

import dev.ted.jitterticket.eventsourced.domain.Event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;

/**
 * Originally copied from https://github.com/Suigi/event-sourced-tic-tac-toe/blob/main/src/main/java/ninja/ranner/xogame/adapter/in/web/EventView.java
 */
public class EventView {

    private final String eventName;
    private final int eventSequence;
    private final List<FieldView> fields;

    private EventView(String eventName, int eventSequence, List<FieldView> fields) {
        this.eventName = eventName;
        this.eventSequence = eventSequence;
        this.fields = fields;
    }

    public static EventView of(Event event) {
        return new EventView(
                event.getClass().getSimpleName(),
                event.eventSequence(), mapFields(event)
        );
    }

    private static List<FieldView> mapFields(Event event) {
        return Arrays.stream(event.getClass().getRecordComponents())
                     .skip(1) // skip the first component as it will always be the primary key/aggregate ID
                     .map(rc -> new FieldView(rc.getName(), extractFieldValue(event, rc)))
                     .toList();
    }

    private static String extractFieldValue(Event event, RecordComponent e) {
        try {
            return e.getAccessor().invoke(event).toString();
        } catch (IllegalAccessException | InvocationTargetException ex) {
            return ex.toString();
        }
    }

    public String eventName() {
        return eventName;
    }

    public List<FieldView> fields() {
        return fields;
    }

    public int eventSequence() {
        return eventSequence;
    }

    public record FieldView(String name, String value) {}

    @Override
    public String toString() {
        return "EventView{" +
               "eventName='" + eventName + '\'' +
               ", fields=" + fields +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        EventView eventView = (EventView) o;
        return eventName.equals(eventView.eventName) && fields.equals(eventView.fields);
    }

    @Override
    public int hashCode() {
        int result = eventName.hashCode();
        result = 31 * result + fields.hashCode();
        return result;
    }
}