package de.maximilianheidenreich.jeventloop.events;

import lombok.Getter;
import lombok.Synchronized;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an event that can be enqueued by the EventLoop at any time.
 */
@Getter
public class AbstractEvent<D> implements Comparable<AbstractEvent<?>> {

    // ======================   VARS

    /**
     * The priority (1 < priority <= 10) used to order events in the {@link java.util.concurrent.PriorityBlockingQueue}.
     * Smaller number = Higher priority.
     *
     */
    private final byte priority;

    /**
     * A unique id which is used internally to map & execute matching callbacks.
     */
    private final UUID id;

    /**
     * Used by the handling logic to determine whether succeeding handlers will be skipped.
     */
    private boolean canceled;

    /**
     * All registered callbacks.
     */
    private final Set<CompletableFuture<D>> callbacks;


    // ======================   CONSTRUCTOR

    /**
     * Creates a new AbstractEvent with a random id and default priority.
     */
    public AbstractEvent() {
        this(UUID.randomUUID());
    }

    /**
     * Creates a new AbstractEvent with the given priority.
     *
     * @param priority
     *          The priority to use (1 < priority <= 10)
     */
    public AbstractEvent(byte priority) {
        this(UUID.randomUUID(), priority);
    }

    /**
     * Creates a new AbstractEvent with the given id and default priority.
     *
     * @param id
     *          The id to use
     */
    public AbstractEvent(UUID id) {
        this(id, (byte) 5);
    }

    /**
     * Creates a new AbstractEvent with the given id and priority.
     *
     * @param id
     *          The id to use
     * @param priority
     *          The priority to use (1 < priority <= 10)
     */
    public AbstractEvent(UUID id, byte priority) {
        this.id = id;
        this.priority = priority;
        this.callbacks = new HashSet<>();
    }


    // ======================   CALLBACK MANAGEMENT

    /**
     * Adds a callback which can be completed or excepted using the respective methods.
     *
     * @param callback
     *          The callback itself
     */
    @Synchronized
    public void addCallback(CompletableFuture<D> callback) {
        if (callback == null) return;
        getCallbacks().add(callback);
    }

    /**
     * Removes a callback specified by id.
     *
     * @param callback
     *          The callback which will be removed
     * @return
     *          {@code true} if the callback was actually removed, {@code false} if not
     */
    @Synchronized
    public boolean removeCallback(CompletableFuture<D> callback) {
        return getCallbacks().remove(callback);
    }


    // ======================   HELPERS

    /**
     * Completes all registered callbacks.
     *
     * @param data
     *          The data to pass back to the callbacks
     */
    public void complete(D data) {
        getCallbacks().forEach(c -> c.complete(data));
    }

    /**
     * Completes all registered callbacks without data.
     * -> Should only be used when using AbstractEvent<Void>
     */
    public void complete() {
        getCallbacks().forEach(c -> c.complete(null));
    }

    /**
     * Excepts all registered callbacks.
     *
     * @param throwable
     *          The reason why except was called
     */
    public void except(Throwable throwable) {
        getCallbacks().forEach(c -> c.completeExceptionally(throwable));
    }

    /**
     * Mark the event as canceled.
     * This will stop succeeding handlers form being executed.
     */
    public void cancel() {
        this.canceled = true;
    }

    /**
     * Compares this abstractEvent to another by their priorities.
     * This is used by the abstractEvent loop queue to prioritize different events.
     *
     * @param abstractEvent
     *          The abstractEvent to compare itself with
     * @return
     */
    @Override
    public int compareTo(AbstractEvent abstractEvent) {
        return Integer.compare(abstractEvent.getPriority(), getPriority());
    }

    @Override
    public String toString() {
        return String.format("[%s-(%d)]", this.getClass().getSimpleName(), getPriority());
    }

}
