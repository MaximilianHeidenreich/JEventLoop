package de.maximilianheidenreich.jeventloop.events;

import lombok.Getter;

/**
 * A test event which stores timing data.
 */
public class TimingEvent extends Event<Long> {

    // ======================   VARS

    /**
     * A UNIX timestamp.
     */
    @Getter
    private final long timestamp;


    // ======================   CONSTRUCTOR

    public TimingEvent(long timestamp) {
        super();
        this.timestamp = timestamp;
    }


    // ======================   BUSINESS LOGIC

    /**
     * Calculates the difference between the time of enqueueing and now.
     *
     * @return
     *      The difference in milliseconds.
     */
    public long getCurrentTimeDiff() {
        return System.currentTimeMillis() - getTimestamp();
    }

}
