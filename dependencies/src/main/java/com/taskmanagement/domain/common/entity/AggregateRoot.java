package com.taskmanagement.domain.common.entity;

import com.taskmanagement.domain.common.event.DomainEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Base class for aggregate roots. Records domain events for dispatch after persistence.
 *
 * @param <ID> identifier type
 */
public abstract class AggregateRoot<ID> extends Entity<ID> {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        super(id);
    }

    /**
     * Records an event to be dispatched once the current unit of work succeeds.
     */
    protected void registerEvent(DomainEvent event) {
        if (event != null) {
            domainEvents.add(event);
        }
    }

    /**
     * Returns the pending events and clears the internal buffer.
     * The returned list is an immutable snapshot.
     */
    public List<DomainEvent> pollDomainEvents() {
        if (domainEvents.isEmpty()) {
            return List.of();
        }
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    /**
     * Returns a read-only view of pending events without clearing them.
     */
    public List<DomainEvent> getDomainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}
