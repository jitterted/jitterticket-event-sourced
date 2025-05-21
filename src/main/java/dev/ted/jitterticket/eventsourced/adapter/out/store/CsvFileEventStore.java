package dev.ted.jitterticket.eventsourced.adapter.out.store;

import dev.ted.jitterticket.eventsourced.application.BaseEventStore;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

public class CsvFileEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> {

    public List<String> csvLines = new ArrayList<>();

    private CsvFileEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate, Class<EVENT> eventClass) {
        super(eventsToAggregate);
    }

    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts() {
        return new CsvFileEventStore<>(Concert::reconstitute, ConcertEvent.class);
    }

    static String toCsv(EventDto<? extends Event> originalEventDto) {
        return originalEventDto.getAggregateRootId() + "," + originalEventDto.getEventSequence() + "," + originalEventDto.getEventType() + "," + originalEventDto.getJson();
    }

    EventDto<EVENT> csvToEventDto(String csv) {
        String[] splitCsv = csv.split(",", 4);
        UUID aggregateRootId = UUID.fromString(splitCsv[0]);
        int eventSequence = Integer.parseInt(splitCsv[1]);
        String eventType = splitCsv[2];
        String json = splitCsv[3];

        return new EventDto<>(aggregateRootId, eventSequence, eventType, json);
    }

    @Override
    public void save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        List<EventDto<EVENT>> uncommittedEventDtos =
                uncommittedEvents.map(event -> EventDto.from(
                                         aggregateId.id(),
                                         event.eventSequence(), // max(sequence) from existing events)
                                         event))
                                 .toList();
        uncommittedEventDtos.stream()
                            .map(CsvFileEventStore::toCsv)
                            .forEach(csvLines::add);
    }

    @Override
    public Stream<EVENT> allEvents() {
        return csvLines.stream()
                       .map(this::csvToEventDto)
                       .map(EventDto::toDomain);
    }

    @Override
    protected List<EventDto<EVENT>> eventDtosFor(ID id) {
        return csvLines.stream()
                       .map(this::csvToEventDto)
                       .filter(dto -> dto.getAggregateRootId().equals(id.id()))
                       .toList();
    }
}
