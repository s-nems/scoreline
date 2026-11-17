package io.github.snems.scoreline;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import io.github.snems.scoreline.exception.TeamAlreadyPlayingException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/** The matches in progress on one scoreboard. Immutable: each operation returns the next state, or throws. */
final class LiveMatches {

    private static final Comparator<LiveMatch> SUMMARY_ORDER = Comparator.comparingLong(LiveMatch::startSequence)
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

    MatchDto match(MatchId id) {
        return matches.stream().filter(live -> live.hasId(id)).findFirst().orElseThrow().toDto();
    }

    List<MatchDto> summary() {
        return summary;
    }

    private void requireNotPlaying(Team team) {
        if (matches.stream().anyMatch(live -> live.involves(team))) {
            throw new TeamAlreadyPlayingException(team.name());
        }
    }
}
