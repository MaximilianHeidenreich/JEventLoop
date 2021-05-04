package de.maximilianheidenreich.jeventloop.threading;

import de.maximilianheidenreich.jeventloop.EventLoop;
import de.maximilianheidenreich.jeventloop.events.AbstractEvent;
import de.maximilianheidenreich.jeventloop.utils.ExceptionUtils;
import lombok.Getter;
import lombok.extern.log4j.Log4j;

import java.util.function.Consumer;

/**
 * Gets started from the DispatcherThread class. It handles a dequeued abstractEvent.
 */
@Log4j
@Getter
public class ExecutorThread <D> implements Runnable {

    // ======================   VARS

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
        this.eventLoop = eventLoop;
        this.abstractEvent = abstractEvent;
    }


    // ======================   BUSINESS LOGIC

    @Override
    public void run() {
        log.debug(String.format("[EventLoop] Started new ExecutorThread for %s", getAbstractEvent().toString()));

        byte priority = abstractEvent.getPriority();
        Thread.currentThread().setPriority((priority < 1 || priority > 10) ? 5 : abstractEvent.getPriority());      // Use default priority if event priority is invalid!
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
                log.debug(String.format("[EventLoop] Stopped ExecutorThread %s due to abstractEvent cancellation!", Thread.currentThread()));
                break;
            }

            Consumer<AbstractEvent<D>> handler = (Consumer<AbstractEvent<D>>) rawHandler;

            try { handler.accept(getAbstractEvent()); }
            catch (Exception e) {
                log.error(ExceptionUtils.getStackTraceAsString(e));
            }

        }
    }

}
