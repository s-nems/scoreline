package io.github.snems.scoreline.exception;

/** Thrown when a match cannot be started because one of its teams is in a match in progress. */
public final class TeamAlreadyPlayingException extends IllegalStateException {

    private final String team;

    public TeamAlreadyPlayingException(String team) {
        super("team is already playing a match: %s".formatted(team));
        this.team = team;
    }

    /** Returns the team name from the rejected request, without surrounding whitespace. */
    public String team() {
        return team;
    }
}
