package de.maximilianheidenreich.jeventloop.threading;

import de.maximilianheidenreich.jeventloop.EventLoop;
import de.maximilianheidenreich.jeventloop.events.AbstractEvent;
import de.maximilianheidenreich.jeventloop.utils.ExceptionUtils;
import lombok.Getter;
import org.apache.log4j.Logger;

import java.util.function.Consumer;

/**
 * Gets started from the DispatcherThread class. It handles a dequeued abstractEvent.
 */
@Getter
public class ExecutorThread <D> implements Runnable {

    // ======================   VARS

    /**
     * Internal logger.
     */
    private final Logger logger;

    /**
     * A reference to the abstractEvent loop.
     */
    private final EventLoop eventLoop;

    /**
     * The abstractEvent assigned to this thread.
     */
    private final AbstractEvent<D> abstractEvent;


    // ======================   CONSTRUCTOR

    public ExecutorThread(EventLoop eventLoop, AbstractEvent<D> abstractEvent) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.eventLoop = eventLoop;
        this.abstractEvent = abstractEvent;
    }


    // ======================   BUSINESS LOGIC

    @Override
    public void run() {
        getLogger().debug(String.format("Started new ExecutorThread for %s", getAbstractEvent().toString()));

        Thread.currentThread().setPriority(abstractEvent.getPriority());
        Thread.currentThread().setName(
                String.format(
                        "ExecutorThread for %s | %s",
                        getAbstractEvent().toString(),
                        Thread.currentThread().getName()
                )
        );

        // RET: No handlers for abstractEvent!
        if (!getEventLoop().getHandlers().containsKey(abstractEvent.getClass()))
            return;

        for (Consumer<? extends AbstractEvent<?>> rawHandler : getEventLoop().getHandlers().get(abstractEvent.getClass())) {

            if (getAbstractEvent().isCanceled()) {
                getLogger().debug(String.format("Stopped ExecutorThread %s due to abstractEvent cancellation!", Thread.currentThread()));
                break;
            }

            Consumer<AbstractEvent<D>> handler = (Consumer<AbstractEvent<D>>) rawHandler;

            try { handler.accept(getAbstractEvent()); }
            catch (Exception e) {
               getLogger().error(ExceptionUtils.getStackTraceAsString(e));
            }

        }
    }

}
