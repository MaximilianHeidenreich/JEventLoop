package de.maximilianheidenreich.jeventloop.threading;

import de.maximilianheidenreich.jeventloop.EventLoop;
import de.maximilianheidenreich.jeventloop.events.Event;
import de.maximilianheidenreich.jeventloop.utils.ExceptionUtils;
import lombok.Getter;
import org.apache.log4j.Logger;

import java.util.function.Consumer;

/**
 * Gets started from the DispatcherThread class. It handles a dequeued event.
 */
@Getter
public class ExecutorThread <D> implements Runnable {

    // ======================   VARS

    /**
     * Internal logger.
     */
    private final Logger logger;

    /**
     * A reference to the event loop.
     */
    private final EventLoop eventLoop;

    /**
     * The event assigned to this thread.
     */
    private final Event<D> event;


    // ======================   CONSTRUCTOR

    public ExecutorThread(EventLoop eventLoop, Event<D> event) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.eventLoop = eventLoop;
        this.event = event;
    }


    // ======================   BUSINESS LOGIC

    @Override
    public void run() {
        getLogger().debug(String.format("Started new ExecutorThread for %s", getEvent().toString()));

        Thread.currentThread().setPriority(event.getPriority());
        Thread.currentThread().setName(
                String.format(
                        "ExecutorThread for %s | %s",
                        getEvent().toString(),
                        Thread.currentThread().getName()
                )
        );

        // RET: No handlers for event!
        if (!getEventLoop().getHandlers().containsKey(event.getClass()))
            return;

        for (Consumer<? extends Event<?>> rawHandler : getEventLoop().getHandlers().get(event.getClass())) {

            if (getEvent().isCanceled()) {
                getLogger().debug(String.format("Stopped ExecutorThread %s due to event cancellation!", Thread.currentThread()));
                break;
            }

            Consumer<Event<D>> handler = (Consumer<Event<D>>) rawHandler;

            try { handler.accept(getEvent()); }
            catch (Exception e) {
               getLogger().error(ExceptionUtils.getStackTraceAsString(e));
            }

        }
    }

}
