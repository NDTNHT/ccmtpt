package com.taskmanagement.domain.common.event;

import java.time.Instant;

/**
 * Domain Event interface representing an event that occurred in the domain.
 */
public interface DomainEvent {
    Instant occurredOn();
}
