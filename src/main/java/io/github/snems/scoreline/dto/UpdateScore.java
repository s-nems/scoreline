package io.github.snems.scoreline.dto;

import static java.util.Objects.requireNonNull;

/** The command to replace the score of a match. */
public record UpdateScore(
    MatchId matchId,
    int homeScore,
    int awayScore
) {

    public UpdateScore {
        requireNonNull(matchId);
    }
}
