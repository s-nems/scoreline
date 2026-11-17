package io.github.snems.scoreline.exception;

import io.github.snems.scoreline.dto.MatchId;

/** Thrown when no match in progress has the given id. */
public final class MatchNotFoundException extends IllegalStateException {

    private final MatchId matchId;

    public MatchNotFoundException(MatchId matchId) {
        super("no match in progress with id %s".formatted(matchId.value()));
        this.matchId = matchId;
    }

    /** Returns the id that was not found. */
    public MatchId matchId() {
        return matchId;
    }
}
