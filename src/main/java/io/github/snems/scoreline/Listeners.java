package io.github.snems.scoreline;

import static org.slf4j.LoggerFactory.getLogger;

import io.github.snems.scoreline.event.ScoreboardEvent;
import io.github.snems.scoreline.event.Subscription;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.slf4j.Logger;

/** The listeners of one scoreboard. Safe to subscribe and cancel while an event is being delivered. */
final class Listeners {

    private static final Logger log = getLogger(Listeners.class);

    private final List<Registration> registrations = new CopyOnWriteArrayList<>();

    Subscription subscribe(Consumer<ScoreboardEvent> listener) {
        var registration = new Registration(listener);
        registrations.add(registration);
        log.info("Listener subscribed ({} subscribed)", registrations.size());
        return registration;
    }

    /** Delivers the event to each listener in turn. A listener that throws does not stop the others. */
    void publish(ScoreboardEvent event) {
        for (var registration : registrations) {
            try {
                registration.listener.accept(event);
            } catch (RuntimeException failure) {
                log.error("Listener failed on {}", event, failure);
            }
        }
    }

    /** Compared by identity, so the same listener subscribed twice is two registrations. */
    private final class Registration implements Subscription {

        private final Consumer<ScoreboardEvent> listener;

        private Registration(Consumer<ScoreboardEvent> listener) {
            this.listener = listener;
        }

        @Override
        public void cancel() {
            if (registrations.remove(this)) {
                log.info("Listener cancelled ({} subscribed)", registrations.size());
            }
        }
    }
}
