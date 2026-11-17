package io.github.snems.scoreline.event;

import io.github.snems.scoreline.dto.MatchDto;

/** The score of a match was replaced. The match carries the new score. */
public record ScoreUpdated(MatchDto match) implements ScoreboardEvent {
}
