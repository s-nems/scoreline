package io.github.snems.scoreline;

import static java.util.Objects.requireNonNull;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import io.github.snems.scoreline.dto.StartMatch;
import io.github.snems.scoreline.dto.UpdateScore;
import io.github.snems.scoreline.exception.MatchNotFoundException;
import io.github.snems.scoreline.exception.TeamAlreadyPlayingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * The football matches in progress.
 *
 * <p>A match is identified by the {@link MatchId} it gets when it is started. A team is identified by its name,
 * ignoring case and surrounding whitespace, and plays one match at a time. A {@code null} argument is rejected
 * with {@link NullPointerException}.
 */
public final class Scoreboard {

    private final ReentrantLock writeLock = new ReentrantLock();

    /** Each change replaces this with the next immutable state, so a reader takes no lock and sees a complete board. */
    private volatile LiveMatches matches = LiveMatches.empty();

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
            return next.match(matchId);
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
            var next = current.updateScore(matchId, score);
            matches = next;
            return next.match(matchId);
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

    private MatchDto underWriteLock(Supplier<MatchDto> change) {
        writeLock.lock();
        try {
            return change.get();
        } finally {
            writeLock.unlock();
        }
    }
}
