package de.maximilianheidenreich.jeventloop;

import de.maximilianheidenreich.jeventloop.events.AbstractEvent;
import de.maximilianheidenreich.jeventloop.events.TimingEvent;
import de.maximilianheidenreich.jeventloop.threading.DispatcherThread;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * An EventLoop that can dispatch events and
 */
@Getter
@Setter
public class EventLoop {

    // ======================   VARS

    /**
     * Internal logger.
     */
    private final Logger logger;

    /**
     * Whether the eventLoop is running.
     */
    private boolean running;

    /**
     * Stores new events. Gets consumed by task executor thread pool.
     */
    private final BlockingQueue<AbstractEvent<?>> abstractEventQueue;

    /**
     * The dispatcher which consumes the queue and.
     */
    private ExecutorService dispatchExecutor;

    /**
     * The executor which dispatched event code.
     */
    private ExecutorService taskExecutor;


    /**
     * All registered handlers which will be executed if an event with matching class is dequeued.
     */
    private final Map<Class<? extends AbstractEvent<?>>, List<Consumer<? extends AbstractEvent<?>>>> handlers;


    // ======================   CONSTRUCTOR

    public EventLoop(ExecutorService dispatchExecutor, ExecutorService taskExecutor) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.running = false;
        this.abstractEventQueue = new PriorityBlockingQueue<>();
        this.dispatchExecutor = dispatchExecutor;
        this.taskExecutor = taskExecutor;
        this.handlers = new HashMap<>();
    }
    public EventLoop() {
        this(Executors.newSingleThreadExecutor(), Executors.newWorkStealingPool());
    }


    // ======================   HANDLER MANAGEMENT

    /**
     * Adds a handler function which will get executed once an AbstractEvent with the matching clazz is dispatched.
     *
     * @param clazz
     *          The class identifying the event for which the handler will be executed
     * @param handler
     *          The handler function
     */
    @Synchronized
    public <E extends AbstractEvent<?>> void addEventHandler(Class<E> clazz, Consumer<E> handler) {

        // Todo: Check null?

        if (!getHandlers().containsKey(clazz))
            getHandlers().put(clazz, new ArrayList<>());

        getHandlers().get(clazz).add(handler);
    }

    /**
     * Removed a handler.
     *
     * @param clazz
     *          The class associated the handler is associated with
     * @param handler
     *          The handler function
     * @return
     *          {@code true} if the handler was actually removed | {@code false} if no matching handler was registered
     */
    @Synchronized
    public <E extends AbstractEvent<?>> boolean removeEventHandler(Class<E> clazz, Consumer<E> handler) {

        // RET: No handlers for class
        if (!getHandlers().containsKey(clazz))
            return false;

        return getHandlers().get(clazz).remove(handler);

    }


    // ======================   RUNTIME

    /**
     * Dispatches an event which will get handled some time in the future.
     *
     * @param event
     *          The event to dispatch
     * @return
     *          A default callback | {@code null} If the event is invalid.
     */
    public <D, E extends AbstractEvent<D>> CompletableFuture<D> dispatch(E event) {

        // RET: Invalid event.
        if (event.getId() == null) return null;

        CompletableFuture<D> callback = new CompletableFuture<>();
        event.addCallback(callback);
        getAbstractEventQueue().add(event);
        return callback;
    }


    // ======================   HELPERS

    /**
     * Starts the dispatcher thread.
     */
    public void start() {

        // RET: Already running!
        if (isRunning()) return;

        getDispatchExecutor().submit(new DispatcherThread(this));
        setRunning(true);
    }

    /**
     * Calls shutdown() on dispatcher & executor executorService.
     * Note: THis does not instantly shut them down.
     */
    public void stop() {

        // RET: Not running.
        if (!isRunning()) return;

        getDispatchExecutor().shutdown();
        getTaskExecutor().shutdown();
        dispatch(new TimingEvent(0));   // Dirty hack to make sure the dispatcher can at least pull 1 last event from the queue and shuts down.
        setRunning(false);
    }

}
