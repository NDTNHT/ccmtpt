package com.taskmanagement.domain.common.entity;

import java.util.Objects;

/**
 * Base class for domain entities. Equality is based on identity.
 *
 * @param <ID> identifier type
 */
public abstract class Entity<ID> {
    protected final ID id;

    protected Entity(ID id) {
        if (id == null) {
            throw new IllegalArgumentException("Entity ID cannot be null");
        }
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    @Override
    public final boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        Entity<?> entity = (Entity<?>) other;
        return Objects.equals(id, entity.id);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(getClass(), id);
    }
}
