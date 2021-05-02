package de.maximilianheidenreich.jeventloop.events;

import lombok.Getter;
import lombok.Synchronized;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Represents an event that can be enqueued by the EventLoop at any time.
 */
@Getter
public class Event <D> implements Comparable<Event<?>> {

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

    public Event() {
        this(UUID.randomUUID());
    }
    public Event(byte priority) {
        this(UUID.randomUUID(), priority);
    }
    public Event(UUID id) {
        this(id, (byte) 5);
    }
    public Event(UUID id, byte priority) {
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
     * -> Should only be used when using Event<Void>
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
     * Compares this event to another by their priorities.
     * This is used by the event loop queue to prioritize different events.
     *
     * @param event
     *          The event to compare itself with
     * @return
     */
    @Override
    public int compareTo(Event event) {
        return Integer.compare(event.getPriority(), getPriority());
    }

    @Override
    public String toString() {
        return String.format("[%s-(%d)]", this.getClass().getSimpleName(), getPriority());
    }

}
