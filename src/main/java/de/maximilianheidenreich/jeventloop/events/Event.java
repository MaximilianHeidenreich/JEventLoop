package de.maximilianheidenreich.jeventloop.events;

import lombok.Getter;

import java.util.UUID;

/**
 * Represents an Event that can be queued by the EventLoop at any time.
 */
public class Event implements Comparable<Event> {

    // ======================   VARS

    /**
     * The priority used to order events in the {@link java.util.concurrent.PriorityBlockingQueue}.
     * Higher number = Higher priority.
     */
    @Getter
    private final int priority;

    /**
     * A unique id which is used internally to map & execute matching callbacks.
     */
    @Getter
    private final UUID id;

    // ======================   CONSTRUCTOR

    public Event() {
        this(UUID.randomUUID());
    }
    public Event(int priority) {
        this(UUID.randomUUID(), priority);
    }
    public Event(UUID id) {
        this(id, 0);
    }
    public Event(UUID id, int priority) {
        this.id = id;
        this.priority = priority;
    }


    // ======================   HELPERS

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
        return String.format("Event(%d)_%s", getPriority(), this.getClass().getSimpleName());
    }

}
