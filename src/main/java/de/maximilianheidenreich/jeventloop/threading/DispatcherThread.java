package de.maximilianheidenreich.jeventloop.threading;

import de.maximilianheidenreich.jeventloop.EventLoop;
import de.maximilianheidenreich.jeventloop.events.Event;
import lombok.Getter;
import org.apache.log4j.Logger;

/**
 * The dispatcher thread takes items from the eventQueue and dispatches a new executor task.
 */
@Getter
public class DispatcherThread implements Runnable {

    // ======================   VARS

    /**
     * Internal logger.
     */
    private final Logger logger;

    /**
     * A reference to the event loop.
     */
    private final EventLoop eventLoop;


    // ======================   CONSTRUCTOR

    public DispatcherThread(EventLoop eventLoop) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.eventLoop = eventLoop;
    }


    // ======================   BUSINESS LOGIC

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Event<?> event = getEventLoop().getEventQueue().take();
                getEventLoop().getTaskExecutor().submit(new ExecutorThread<>(getEventLoop(), event));
            }
        } catch (InterruptedException e) {
            getLogger().debug("Interrupted DispatcherThead " + Thread.currentThread());
        }
    }

}
