package de.maximilianheidenreich.jeventloop;

import de.maximilianheidenreich.jeventloop.events.Event;
import lombok.Getter;
import org.apache.log4j.Logger;

/**
 * Stores data & API utilities related to the handling of an event.
 */
public class EventHandle {

    // ======================   VARS

    /**
     * A reference to the EventLoop which can be used in handlers if needed.
     */
    @Getter
    private final EventLoop eventLoop;

    /**
     * The event which is handled.
     */
    @Getter
    private final Event event;

    /**
     * Will break the execution chain of event handlers if set to true.
     */
    @Getter
    private boolean canceled;

    // ======================   CONSTRUCTOR

    public EventHandle(EventLoop eventLoop, Event event) {
        this.eventLoop = eventLoop;
        this.event = event;
        this.canceled = false;
    }


    // ======================   HELPERS

    /**
     * Breaks the execution chain of event handlers.
     */
    public void cancel() {
        this.canceled = true;
    }

    /**
     * Wrapper to easily call stores callbacks with some data.
     *
     * @param data
     *          The data which will be passed to the callbacks
     * @return
     */
    public boolean callback(Object data) {
        return getEventLoop().handleCallbacks(getEvent().getId(), data);
    }

    /**
     * Wrapper to easily get the EventLoop logger.
     * This is useful for times when you want to log stuff in your handlers but you dont want to create and
     * manage references to a custom {@link Logger} instance.
     *
     * @return
     *          The Logger used by the EventLoop
     */
    public Logger getLogger() {
        return getEventLoop().getLogger();
    }

}
