package io.github.snems.scoreline;

import static org.slf4j.LoggerFactory.getLogger;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import io.github.snems.scoreline.exception.MatchNotFoundException;
import io.github.snems.scoreline.exception.TeamAlreadyPlayingException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;

/** The matches in progress on one scoreboard. Immutable: each operation returns the next state, or throws. */
final class LiveMatches {

    private static final Logger log = getLogger(LiveMatches.class);

    private static final Comparator<LiveMatch> SUMMARY_ORDER = Comparator.comparingInt(
            (LiveMatch live) -> live.score().total())
            .thenComparingLong(LiveMatch::startSequence)
            .reversed();

    private static final LiveMatches EMPTY = new LiveMatches(List.of(), 0);

    /** Kept in summary order. */
    private final List<LiveMatch> matches;

    private final long nextStartSequence;

    private final List<MatchDto> summary;

    private LiveMatches(List<LiveMatch> matches, long nextStartSequence) {
        this.matches = matches;
        this.nextStartSequence = nextStartSequence;
        this.summary = matches.stream().map(LiveMatch::toDto).toList();
    }

    static LiveMatches empty() {
        return EMPTY;
    }

    LiveMatches start(MatchId id, Team home, Team away) {
        var starting = new LiveMatch(id, home, away, Score.GOALLESS, nextStartSequence);
        requireNotPlaying(home);
        requireNotPlaying(away);
        var next = Stream.concat(matches.stream(), Stream.of(starting))
                .sorted(SUMMARY_ORDER)
                .toList();
        return new LiveMatches(next, nextStartSequence + 1);
    }

    LiveMatches updateScore(MatchId id, Score score) {
        var current = inProgress(id);
        var updated = current.withScore(score);
        var next = matches.stream()
                .map(live -> live.hasId(id) ? updated : live)
                .sorted(SUMMARY_ORDER)
                .toList();
        return new LiveMatches(next, nextStartSequence);
    }

    LiveMatches finish(MatchId id) {
        var finished = inProgress(id);
        var next = matches.stream().filter(live -> !live.equals(finished)).toList();
        return new LiveMatches(next, nextStartSequence);
    }

    MatchDto match(MatchId id) {
        return inProgress(id).toDto();
    }

    List<MatchDto> summary() {
        return summary;
    }

    private LiveMatch inProgress(MatchId id) {
        return matches.stream().filter(live -> live.hasId(id)).findFirst().orElseThrow(() -> {
            log.warn("Rejected request, no match in progress with id {}", id.value());
            return new MatchNotFoundException(id);
        });
    }

    private void requireNotPlaying(Team team) {
        if (matches.stream().anyMatch(live -> live.involves(team))) {
            log.warn("Rejected start, team already playing: {}", team.name());
            throw new TeamAlreadyPlayingException(team.name());
        }
    }
}
