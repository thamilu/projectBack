package com.eshop.app.core.kernel;

import java.util.function.Consumer;

/**
 * Utility for managing JPA bidirectional relationship synchronization.
 *
 * <p>Eliminates repetitive null-check patterns across all entities that manage bidirectional
 * OneToOne/OneToMany associations.
 *
 * <h3>Usage:</h3>
 *
 * <pre>{@code
 * BiDirectionalRelationship.sync(
 *     this.child,               // current child
 *     newChild,                 // incoming child
 *     c -> c.setParent(null),   // unlink old
 *     c -> c.setParent(this)    // link new
 * );
 * this.child = newChild;
 * }</pre>
 */
public final class BiDirectionalRelationship {

    private BiDirectionalRelationship() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Synchronizes a bidirectional relationship.
     *
     * <p>Safely unlinks the current entity (if different from incoming) and links the incoming
     * entity.
     *
     * @param current currently associated entity (may be null)
     * @param incoming new entity to associate (may be null)
     * @param unlink action to unlink from current (e.g., child.setParent(null))
     * @param link action to link to incoming (e.g., child.setParent(this))
     * @param <T> child entity type
     */
    public static <T> void sync(T current, T incoming, Consumer<T> unlink, Consumer<T> link) {
        if (current != null && !current.equals(incoming)) {
            unlink.accept(current);
        }
        if (incoming != null) {
            link.accept(incoming);
        }
    }
}
