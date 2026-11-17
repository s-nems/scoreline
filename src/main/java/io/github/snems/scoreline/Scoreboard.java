package io.github.snems.scoreline;

import static java.util.Objects.requireNonNull;
import static org.slf4j.LoggerFactory.getLogger;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import io.github.snems.scoreline.dto.StartMatch;
import io.github.snems.scoreline.dto.UpdateScore;
import io.github.snems.scoreline.event.MatchFinished;
import io.github.snems.scoreline.event.MatchStarted;
import io.github.snems.scoreline.event.ScoreUpdated;
import io.github.snems.scoreline.event.ScoreboardEvent;
import io.github.snems.scoreline.event.Subscription;
import io.github.snems.scoreline.exception.MatchNotFoundException;
import io.github.snems.scoreline.exception.TeamAlreadyPlayingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;

/**
 * The football matches in progress.
 *
 * <p>A match is identified by the {@link MatchId} it gets when it is started. A team is identified by its name,
 * ignoring case and surrounding whitespace, and plays one match at a time. A {@code null} argument is rejected
 * with {@link NullPointerException}.
 *
 * <p>A scoreboard is safe to use from several threads, and a summary is a consistent snapshot of the board.
 *
 * <p>A listener gets one event for each change, in the order the changes happened, on the thread that made the
 * change and before that change returns. So a listener must be quick, and it must not change the scoreboard: a
 * start, update or finish that a listener makes on the thread it runs on is rejected with
 * {@link IllegalStateException}. A listener that throws an exception does not affect the change or the other
 * listeners.
 */
public final class Scoreboard {

    private static final Logger log = getLogger(Scoreboard.class);

    private final ReentrantLock writeLock = new ReentrantLock();

    /** Each change replaces this with the next immutable state, so a reader takes no lock and sees a complete board. */
    private volatile LiveMatches matches = LiveMatches.empty();

    private final Listeners listeners = new Listeners();

    public Scoreboard() {
        log.info("Scoreboard created");
    }

    /**
     * Starts a match at 0-0 and gives it a new id.
     *
     * @throws IllegalArgumentException if a name is blank or both names are the same team
     * @throws TeamAlreadyPlayingException if either team is in a match in progress, at home or away
     */
    public MatchDto start(StartMatch startMatch) {
        requireNonNull(startMatch);
        var home = new Team(startMatch.homeTeam());
        var away = new Team(startMatch.awayTeam());
        var matchId = new MatchId(UUID.randomUUID());
        return underWriteLock(() -> {
            var current = matches;
            var next = current.start(matchId, home, away);
            matches = next;
            var started = next.match(matchId);
            log.info("Match started: {} ({} in progress)", started, next.summary().size());
            listeners.publish(new MatchStarted(started));
            return started;
        });
    }

    /**
     * Replaces the score of a match in progress. The new score may be lower, which is how a goal is corrected.
     *
     * @throws IllegalArgumentException if a score is negative or both together do not fit in an {@code int}
     * @throws MatchNotFoundException if no match in progress on this scoreboard has that id
     */
    public MatchDto update(UpdateScore updateScore) {
        requireNonNull(updateScore);
        var matchId = updateScore.matchId();
        var score = new Score(updateScore.homeScore(), updateScore.awayScore());
        return underWriteLock(() -> {
            var current = matches;
            var previous = current.match(matchId);
            var next = current.updateScore(matchId, score);
            matches = next;
            var updated = next.match(matchId);
            log.info("Score updated: {} (was {} - {})", updated, previous.homeScore(), previous.awayScore());
            listeners.publish(new ScoreUpdated(updated));
            return updated;
        });
    }

    /**
     * Removes a match in progress and returns it with its final score. The scoreboard keeps nothing about a
     * finished match.
     *
     * @throws MatchNotFoundException if no match in progress on this scoreboard has that id
     */
    public MatchDto finish(MatchId matchId) {
        requireNonNull(matchId);
        return underWriteLock(() -> {
            var current = matches;
            var finished = current.match(matchId);
            var next = current.finish(matchId);
            matches = next;
            log.info("Match finished: {} ({} in progress)", finished, next.summary().size());
            listeners.publish(new MatchFinished(finished));
            return finished;
        });
    }

    /**
     * Lists the matches in progress by total score, highest first, then most recently started first. The list is
     * an unmodifiable snapshot.
     */
    public List<MatchDto> summary() {
        return matches.summary();
    }

    /**
     * Delivers every later change to the listener, until the returned subscription is cancelled. The same listener
     * subscribed twice gets each event twice.
     */
    public Subscription subscribe(Consumer<ScoreboardEvent> listener) {
        requireNonNull(listener);
        return listeners.subscribe(listener);
    }

    private MatchDto underWriteLock(Supplier<MatchDto> change) {
        if (writeLock.isHeldByCurrentThread()) {
            log.warn("Rejected change made from inside a listener");
            throw new IllegalStateException("a listener must not change the scoreboard");
        }
        writeLock.lock();
        try {
            return change.get();
        } finally {
            writeLock.unlock();
        }
    }
}
