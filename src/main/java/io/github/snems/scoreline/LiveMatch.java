package io.github.snems.scoreline;

import static org.slf4j.LoggerFactory.getLogger;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import org.slf4j.Logger;

/** A match in progress and its place in the start order. */
record LiveMatch(
    MatchId id,
    Team home,
    Team away,
    Score score,
    long startSequence
) {

    private static final Logger log = getLogger(LiveMatch.class);

    LiveMatch {
        if (home.isSameTeamAs(away)) {
            log.warn("Rejected match of a team against itself: {}", home.name());
            throw new IllegalArgumentException(
                    "home and away must be different teams: %s, %s".formatted(home.name(), away.name()));
        }
    }

    boolean hasId(MatchId id) {
        return this.id.equals(id);
    }

    boolean involves(Team team) {
        return home.isSameTeamAs(team) || away.isSameTeamAs(team);
    }

    LiveMatch withScore(Score score) {
        return new LiveMatch(id, home, away, score, startSequence);
    }

    MatchDto toDto() {
        return new MatchDto(id, home.name(), away.name(), score.home(), score.away());
    }
}
