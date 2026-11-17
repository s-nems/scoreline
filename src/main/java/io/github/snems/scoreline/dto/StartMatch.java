package io.github.snems.scoreline.dto;

import static java.util.Objects.requireNonNull;

/** The command to start a match. The scoreboard validates the names. */
public record StartMatch(String homeTeam, String awayTeam) {

    public StartMatch {
        requireNonNull(homeTeam);
        requireNonNull(awayTeam);
    }
}
