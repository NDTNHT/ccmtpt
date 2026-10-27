package com.taskmanagement.application.port;

import com.taskmanagement.domain.common.event.DomainEvent;

import java.util.List;

/**
 * Publishes domain events after a unit of work has been persisted successfully.
 */
@FunctionalInterface
public interface DomainEventDispatcher {
    void dispatch(List<DomainEvent> events);
}
