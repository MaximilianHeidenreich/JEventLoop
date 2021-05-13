package de.maximilianheidenreich.jeventloop.threading;

import de.maximilianheidenreich.jeventloop.EventLoop;
import de.maximilianheidenreich.jeventloop.events.AbstractEvent;
import lombok.Getter;
import lombok.extern.java.Log;

/**
 * The dispatcher thread takes items from the eventQueue and dispatches a new executor task.
 */
@Log
@Getter
public class DispatcherThread implements Runnable {

    // ======================   VARS

    /**
     * A reference to the event loop.
     */
    private final EventLoop eventLoop;


    // ======================   CONSTRUCTOR

    public DispatcherThread(EventLoop eventLoop) {
        this.eventLoop = eventLoop;
    }


    // ======================   BUSINESS LOGIC

    @Override
    public void run() {
        log.fine(String.format("[EventLoop] Started new DispatcherThread %s", Thread.currentThread()));

        try {
            while (!Thread.currentThread().isInterrupted() && !getEventLoop().getDispatchExecutor().isShutdown()) {
                AbstractEvent<?> abstractEvent = getEventLoop().getAbstractEventQueue().take();
                getEventLoop().getTaskExecutor().submit(new ExecutorThread<>(getEventLoop(), abstractEvent));
            }
        } catch (InterruptedException e) {
            log.fine("[EventLoop] Interrupted DispatcherThead " + Thread.currentThread());
        }
    }

}
