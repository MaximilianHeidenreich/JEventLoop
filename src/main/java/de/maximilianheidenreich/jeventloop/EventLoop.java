package de.maximilianheidenreich.jeventloop;

import de.maximilianheidenreich.jeventloop.events.Event;
import de.maximilianheidenreich.jeventloop.exceptions.EventCancelException;
import de.maximilianheidenreich.jeventloop.util.ThrowingFunction;
import lombok.Getter;
import lombok.NonNull;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * An EventLoop which supports async callbacks and handles queued events using an {@link ExecutorService}.
 */
public class EventLoop implements Runnable {

    // ======================   VARS

    /**
     * The logger for this class.
     */
    @Getter
    private final Logger logger;

    /**
     * Stores events to be consumed inside the loop.
     */
    @Getter
    private final BlockingQueue<Event> eventQueue;

    /**
     * Stores a callback Consumer for a given event id.
     */
    @Getter
    private final Map<UUID, List<CompletableFuture<Object>>> callbackHandlers;

    /**
     * Stores all registered handlers for specific events.
     */
    @Getter
    private final Map<Class<? extends Event>, List<ThrowingFunction<EventHandle, EventHandle>>> eventHandlers;

    /**
     * The ExecutorService which will be used to handle events & callbacks.
     */
    @Getter
    private final ExecutorService executorService;


    // ======================   CONSTRUCTOR

    /**
     * Creates a new EventLoop which uses a new default {@link ForkJoinPool} for task execution.
     */
    public EventLoop() {
        this(Executors.newWorkStealingPool());
    }

    /**
     * Creates a new EventLoop which uses the specified {@link ExecutorService} for task execution.
     *
     * @param executorService
     *          The ExecutorService to use
     */
    public EventLoop(ExecutorService executorService) {
        this.logger = Logger.getLogger(this.getClass().getName());
        this.eventQueue = new PriorityBlockingQueue<>();
        this.callbackHandlers = new ConcurrentHashMap<>();
        this.eventHandlers = new ConcurrentHashMap<>();
        this.executorService = executorService;
    }


    // ======================   EVENT LOOP

    /**
     * Executes the event loop. Responsible for de-queueing events and passing them to {@link this#handleEvent(Event)}.
     */
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Event event = getEventQueue().take();
                handleEvent(event);
                /*Event event = getEventQueue().poll(2, TimeUnit.MILLISECONDS); // TODO: Extract to own impl class?
                if (event != null)
                    handleEvent(event);*/
                    //getExecutorService().submit(() -> handleEvent(event));
            }
        }
        catch (InterruptedException e) {
            getLogger().debug("Interrupted thead " + Thread.currentThread().toString());
        }
    }

    /**
     * Calls all stored event handlers for the given event.
     *
     * @param event
     *          The event which handlers to call
     */
    public void handleEvent(@NonNull Event event) {
        getLogger().trace("Handling " + event.toString());

        // RET: No handlers for event!
        if (!getEventHandlers().containsKey(event.getClass()))
            return;

        CompletableFuture<EventHandle> handlerPipeline = CompletableFuture.supplyAsync(
                () -> new EventHandle(this, event),
                getExecutorService()
        );

        for (Function<EventHandle, EventHandle> eventHandler : getEventHandlers().get(event.getClass())) {

            handlerPipeline
                    .thenApply(eventHandler)        // TODO: USe async faster with multiple handlers?
                    .thenApply((handle) -> {

                        if (handle.isCanceled())
                            throw new RuntimeException(new EventCancelException(handle.getEvent() + " was canceled!"));
                        else
                            return handle;

                    })
                    .exceptionally(throwable -> {       // TODO: Add EventHandle exception which adds throwing class & method name?
                        getLogger().error(throwable);
                        if (getLogger().isTraceEnabled()) throwable.printStackTrace();  // TODO: remove?
                        throw new RuntimeException(throwable);
                    });

        }

    }

    /**
     * Completes all stored callback handlers with the given data object.
     *
     * @param id
     *          The unique id that identifies the handlers (Ususally the event id)
     * @param object
     *          The data object which is passed onto {@link CompletableFuture#complete(Object)}
     * @return
     *          {@code true} if any callbacks were submitted for execution, {@code false} if none were stored.
     */
    public boolean handleCallbacks(UUID id, Object object) {
        getLogger().trace("Handling callbacks for: " + id);

        // RET: No handlers for event!
        if (!getCallbackHandlers().containsKey(id))
            return false;

        for (Iterator<CompletableFuture<Object>> iterator = getCallbackHandlers().get(id).iterator(); iterator.hasNext();) {
            CompletableFuture<Object> callback = iterator.next();
            getExecutorService().submit(() ->callback.complete(object));        // Call directly in this thread??
            iterator.remove();
        }

        return true;

    }


    // ======================   HANDLER MANAGEMENT

    /**
     * Adds a handler to the {@link List} of currently stored handlers. The handler will get executed
     * once an {@link Event} which extends the specified class gets handled by the event loop.
     *
     * @param clazz
     *          The associated event class of this handler
     * @param handler
     *          The handler itself
     */
    public <P extends Event> void addEventHandler(Class<P> clazz, @NonNull ThrowingFunction<EventHandle, EventHandle> handler) {

        if (!getEventHandlers().containsKey(clazz))
            getEventHandlers().put(clazz, new ArrayList<>());

        getEventHandlers().get(clazz).add(handler);

    }

    /**
     * Removes a handler from the {@link List} of currently stored handlers.
     *
     * @param clazz
     *          The associated event class of this handler
     * @param handler
     *          The handler itself
     */
    public <P extends Event> boolean removeEventHandler(Class<P> clazz, @NonNull ThrowingFunction<EventHandle, EventHandle> handler) {

        // RET: No handlers for class
        if (!getEventHandlers().containsKey(clazz))
            return false;

        return getEventHandlers().get(clazz).remove(handler);

    }

    /**
     * Adds a callback specified by an id. These can be executed using the
     * {@link this#handleCallbacks(UUID, Object)} method.
     *
     * @param id
     *          The unique id which usually is the event id
     * @param callback
     *          The callback on which {@link CompletableFuture#complete(Object)} will get called
     */
    public void addCallbackHandler(UUID id, CompletableFuture<Object> callback) {

        if (!getCallbackHandlers().containsKey(id))
            getCallbackHandlers().put(id, new ArrayList<>());

        getCallbackHandlers().get(id).add(callback);

    }

    /**
     * Removes a callback specified by id.
     *
     * @param id
     *          The unique id which usually is the event id
     * @param callback
     *          The callback which will be removed
     * @return
     *          {@code true} if the callback was removed, {@code false} if not
     */
    public boolean removeCallbackHandler(UUID id, CompletableFuture<Object> callback) {

        // RET: No handlers for class
        if (!getCallbackHandlers().containsKey(id))
            return false;

        return getCallbackHandlers().get(id).remove(callback);

    }


    // ======================   HELPERS

    /**
     * Starts the EventLoop in a separate thread.
     */
    public void start() {
        getExecutorService().submit(this);
    }

    /**
     * Stops the EventLoop by shutting down the {@link ExecutorService} in use.
     */
    public void stop() {
        getExecutorService().shutdown();
    }

    /**
     * Adds an event to the event queue.
     *
     * @param event
     *          The event to enqueue
     */
    public void queueEvent(@NonNull Event event) {
        getEventQueue().add(event);
    }

    /**
     * Adds an event to the event queue and returns a new {@link CompletableFuture} callback.
     * The callback can be completed from inside an event handler by calling {@link EventHandle#callback(Object)}.
     *
     * @param event
     *          The event to enqueue
     * @return
     *          The new CompletableFuture callback
     */
    public CompletableFuture<Object> queueAsyncEvent(@NonNull Event event) {
        CompletableFuture<Object> callback = new CompletableFuture<>();
        addCallbackHandler(event.getId(), callback);
        queueEvent(event);
        return callback;
    }

}
