package io.github.snems.scoreline.dto;

/** A match as it stood when the scoreboard returned it. */
public record MatchDto(
    MatchId id,
    String homeTeam,
    String awayTeam,
    int homeScore,
    int awayScore
) {
}
