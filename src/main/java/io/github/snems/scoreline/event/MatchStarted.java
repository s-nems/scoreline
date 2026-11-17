package io.github.snems.scoreline.event;

import io.github.snems.scoreline.dto.MatchDto;

/** A match was started. It stands at 0-0. */
public record MatchStarted(MatchDto match) implements ScoreboardEvent {
}
