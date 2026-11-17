package io.github.snems.scoreline.event;

import io.github.snems.scoreline.dto.MatchDto;

/** A change of a scoreboard, delivered to its listeners. */
public sealed interface ScoreboardEvent permits MatchStarted, ScoreUpdated, MatchFinished {

    /** The match that changed: as started, with its new score, or with its final score. */
    MatchDto match();
}
