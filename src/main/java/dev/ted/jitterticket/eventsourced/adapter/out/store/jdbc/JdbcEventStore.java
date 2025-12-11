package dev.ted.jitterticket.eventsourced.adapter.out.store.jdbc;

import dev.ted.jitterticket.Gatherers;
import dev.ted.jitterticket.eventsourced.adapter.out.store.EventDto;
import dev.ted.jitterticket.eventsourced.application.BaseEventStore;
import dev.ted.jitterticket.eventsourced.application.port.EventStore;
import dev.ted.jitterticket.eventsourced.domain.Event;
import dev.ted.jitterticket.eventsourced.domain.EventSourcedAggregate;
import dev.ted.jitterticket.eventsourced.domain.Id;
import dev.ted.jitterticket.eventsourced.domain.concert.Concert;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertEvent;
import dev.ted.jitterticket.eventsourced.domain.concert.ConcertId;
import dev.ted.jitterticket.eventsourced.domain.customer.Customer;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerEvent;
import dev.ted.jitterticket.eventsourced.domain.customer.CustomerId;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class JdbcEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> {

    private final EventDboRepository eventDboRepository;
    private final Class<EVENT> concreteEventClass;

    private JdbcEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate,
                           EventDboRepository eventDboRepository,
                           Class<EVENT> concreteEventClass) {
        super(eventsToAggregate);
        this.eventDboRepository = eventDboRepository;
        this.concreteEventClass = concreteEventClass;
    }


    //region Creation Methods for Specific Aggregate Types
    public static EventStore<ConcertId, ConcertEvent, Concert> forConcerts(EventDboRepository eventDboRepository) {
        return new JdbcEventStore<>(Concert::reconstitute, eventDboRepository, ConcertEvent.class);
    }

    public static EventStore<CustomerId, CustomerEvent, Customer> forCustomers(EventDboRepository eventDboRepository) {
        return new JdbcEventStore<>(Customer::reconstitute, eventDboRepository, CustomerEvent.class);
    }
    //endregion


    @Override
    protected @Nonnull List<EventDto<EVENT>> eventDtosFor(ID id) {
        return eventDboRepository.findByAggregateRootId(id.id())
                                 .stream()
                                 .map(dbo -> new EventDto<EVENT>(
                                         dbo.getAggregateRootId(),
                                         null, // TODO this null will go away once the highest global event sequence is returned from save()
                                         dbo.getEventType(),
                                         dbo.getJson()))
                                 .toList();
    }

    @Override
    public long save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        // assumes there's at least one uncommittedEvent in the incoming stream!
        List<EventDbo> dbos = uncommittedEvents
                .map(event -> EventDto.from(aggregateId.id(),
                                            null, // TODO this null will go away once the highest global event sequence is returned from save()
                                            event))
                .map(dto -> new EventDbo(
                        dto.getAggregateRootId(),
                        dto.getEventType(),
                        dto.getJson()))
                .toList();
        eventDboRepository.saveAll(dbos);
        EventDbo lastDboToWrite = dbos.getLast();
        Optional<EventDbo> lastEventDboSaved =
                eventDboRepository.findByAggregateRootIdAndEventSequence(
                        lastDboToWrite.getAggregateRootId(),
                        lastDboToWrite.getEventSequence());
        return lastEventDboSaved
                .map(EventDbo::getEventSequence)
                .orElseThrow(() -> new IllegalStateException("Could not find event saved with Event id: " + lastDboToWrite.getAggregateRootId() + " and Event Sequence: " + lastDboToWrite.getEventSequence()));
    }

    @Override
    public Stream<EVENT> allEvents() {
        return mapToDomainEvents(eventDboRepository
                                         .findAllByOrderByEventSequenceAsc());
    }

    @Override
    public Stream<EVENT> allEventsAfter(long globalEventSequence) {
        return mapToDomainEvents(eventDboRepository
                                         .findEventsAfter(globalEventSequence));

    }

    private Stream<EVENT> mapToDomainEvents(List<EventDbo> allByGlobalSequence) {
        return allByGlobalSequence
                .stream()
                .map(dbo -> new EventDto<EVENT>(
                        dbo.getAggregateRootId(),
                        null, // TODO this null will go away once the highest global event sequence is returned from save()
                        dbo.getEventType(),
                        dbo.getJson()))
                .map(EventDto::toDomain)
                .gather(Gatherers.filterAndCastTo(concreteEventClass));
    }
}
