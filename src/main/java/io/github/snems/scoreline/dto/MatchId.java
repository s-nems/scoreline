package io.github.snems.scoreline.dto;

import static java.util.Objects.requireNonNull;

import java.io.Serializable;
import java.util.UUID;

/** The identity of a match, issued by the scoreboard when the match is started. */
public record MatchId(UUID value) implements Serializable {

    public MatchId {
        requireNonNull(value);
    }
}
