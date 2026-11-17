package io.github.snems.scoreline.event;

/** One listener subscribed to a scoreboard. */
public interface Subscription {

    /**
     * Stops delivery to the listener, though an event already being delivered when this is called may still arrive.
     * Cancelling again does nothing.
     */
    void cancel();
}
