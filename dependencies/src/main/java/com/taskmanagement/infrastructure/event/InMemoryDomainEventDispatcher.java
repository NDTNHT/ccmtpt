package com.taskmanagement.infrastructure.event;

import com.taskmanagement.application.port.DomainEventDispatcher;
import com.taskmanagement.domain.common.event.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Synchronous in-memory event dispatcher. A failing subscriber does not affect other
 * subscribers or the persisted use case.
 */
public final class InMemoryDomainEventDispatcher implements DomainEventDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryDomainEventDispatcher.class);

    private final List<Consumer<DomainEvent>> subscribers = new CopyOnWriteArrayList<>();

    public void subscribe(Consumer<DomainEvent> subscriber) {
        subscribers.add(Objects.requireNonNull(subscriber, "Subscriber cannot be null"));
    }

    @Override
    public void dispatch(List<DomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (DomainEvent event : events) {
            for (Consumer<DomainEvent> subscriber : subscribers) {
                try {
                    subscriber.accept(event);
                } catch (RuntimeException exception) {
                    logger.error("Event subscriber failed for {}", event.getClass().getSimpleName(),
                        exception);
                }
            }
        }
    }

    public void clearSubscribers() {
        subscribers.clear();
    }
}
