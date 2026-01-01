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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class JdbcEventStore<ID extends Id, EVENT extends Event, AGGREGATE extends EventSourcedAggregate<EVENT, ID>> extends BaseEventStore<ID, EVENT, AGGREGATE> {

    private static final Logger log = LoggerFactory.getLogger(JdbcEventStore.class);

    private final EventDboRepository eventDboRepository;
    private final Class<EVENT> baseEventClass;

    private JdbcEventStore(Function<List<EVENT>, AGGREGATE> eventsToAggregate,
                           EventDboRepository eventDboRepository,
                           Class<EVENT> baseEventClass) {
        super(eventsToAggregate);
        this.eventDboRepository = eventDboRepository;
        this.baseEventClass = baseEventClass;
        // retrieve concrete subclasses of the (abstract) base event for use in Repository "findEventsAfter" query
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
                                         dbo.getEventSequence(),
                                         dbo.getEventType(),
                                         dbo.getJson()))
                                 .toList();
    }

    @Override
    public Stream<EVENT> save(ID aggregateId, Stream<EVENT> uncommittedEvents) {
        // assumes there's at least one uncommittedEvent in the incoming stream!
        List<EventDbo> dbos = uncommittedEvents
                .map(event -> EventDto.from(aggregateId.id(),
                                            null, // TODO this parameter shouldn't exist as event sequence is assigned upon DB INSERT
                                            event))
                .map(dto -> new EventDbo(
                        dto.getAggregateRootId(),
                        dto.getEventType(),
                        dto.getJson()))
                .toList();
        Iterable<EventDbo> savedEventDbos = eventDboRepository.saveAll(dbos);
        return mapToDomainEvents(StreamSupport.stream(savedEventDbos.spliterator(), false));
    }

    @Override
    public Stream<EVENT> allEvents() {
        return mapToDomainEvents(eventDboRepository
                                         .findAllByOrderByEventSequenceAsc().stream());
    }

    @Override
    public Stream<EVENT> allEventsAfter(long eventSequence) {
        log.info("Fetching List<EventDbo> after event sequence: {}", eventSequence);
        List<EventDbo> eventsAfter = eventDboRepository.findEventsAfter(eventSequence);
        log.info("Fetched {} events, mapping to Domain events", eventsAfter.size());
        Stream<EVENT> domainEvents = mapToDomainEvents(eventsAfter.stream());
        log.info("Mapped all events to Domain events");
        return domainEvents;
    }

    private Stream<EVENT> mapToDomainEvents(Stream<EventDbo> eventDbos) {
        return eventDbos
                .map(dbo -> new EventDto<EVENT>(
                        dbo.getAggregateRootId(),
                        dbo.getEventSequence(),
                        dbo.getEventType(),
                        dbo.getJson()))
                .map(EventDto::toDomain)
                .gather(Gatherers.filterAndCastTo(baseEventClass));
    }
}
