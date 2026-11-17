package io.github.snems.scoreline.event;

import io.github.snems.scoreline.dto.MatchDto;

/** A match was finished and removed from the scoreboard. The match carries the final score. */
public record MatchFinished(MatchDto match) implements ScoreboardEvent {
}
