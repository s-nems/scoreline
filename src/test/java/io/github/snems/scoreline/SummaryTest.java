package io.github.snems.scoreline;

import static io.github.snems.scoreline.SampleMatches.ARGENTINA_AUSTRALIA;
import static io.github.snems.scoreline.SampleMatches.GERMANY_FRANCE;
import static io.github.snems.scoreline.SampleMatches.MEXICO_CANADA;
import static io.github.snems.scoreline.SampleMatches.SPAIN_BRAZIL;
import static io.github.snems.scoreline.SampleMatches.URUGUAY_ITALY;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.StartMatch;
import io.github.snems.scoreline.dto.UpdateScore;
import org.junit.jupiter.api.Test;

class SummaryTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void shouldOrderExampleFromExerciseStatement() {
        // given
        play(MEXICO_CANADA, 0, 5);
        play(SPAIN_BRAZIL, 10, 2);
        play(GERMANY_FRANCE, 2, 2);
        play(URUGUAY_ITALY, 6, 6);
        play(ARGENTINA_AUSTRALIA, 3, 1);

        // when
        var summary = scoreboard.summary();

        // then
        assertThat(summary)
                .extracting(SummaryTest::line)
                .containsExactly(
                        "Uruguay 6 - Italy 6",
                        "Spain 10 - Brazil 2",
                        "Mexico 0 - Canada 5",
                        "Argentina 3 - Australia 1",
                        "Germany 2 - France 2");
    }

    @Test
    void shouldBreakTieByMostRecentStart() {
        // given
        var mexicoCanadaId = scoreboard.start(MEXICO_CANADA).id();
        var spainBrazilId = scoreboard.start(SPAIN_BRAZIL).id();
        var germanyFranceId = scoreboard.start(GERMANY_FRANCE).id();

        // when
        scoreboard.update(new UpdateScore(germanyFranceId, 2, 2));
        scoreboard.update(new UpdateScore(mexicoCanadaId, 4, 0));
        scoreboard.update(new UpdateScore(spainBrazilId, 3, 1));

        // then
        assertThat(scoreboard.summary())
                .extracting(SummaryTest::line)
                .containsExactly("Germany 2 - France 2", "Spain 3 - Brazil 1", "Mexico 4 - Canada 0");
    }

    @Test
    void shouldMoveMatchDownWhenScoreLowered() {
        // given
        var mexicoCanada = play(MEXICO_CANADA, 2, 1);
        play(SPAIN_BRAZIL, 1, 1);

        // when
        scoreboard.update(new UpdateScore(mexicoCanada.id(), 1, 0));

        // then
        assertThat(scoreboard.summary())
                .extracting(SummaryTest::line)
                .containsExactly("Spain 1 - Brazil 1", "Mexico 1 - Canada 0");
    }

    @Test
    void shouldKeepEarlierSummaryUnchanged() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        var spainBrazil = scoreboard.start(SPAIN_BRAZIL);
        var earlier = scoreboard.summary();

        // when
        scoreboard.start(GERMANY_FRANCE);
        scoreboard.update(new UpdateScore(mexicoCanada.id(), 1, 0));
        scoreboard.finish(spainBrazil.id());

        // then
        assertThat(earlier).containsExactly(spainBrazil, mexicoCanada);
    }

    @Test
    void shouldReturnUnmodifiableList() {
        // given
        scoreboard.start(MEXICO_CANADA);

        // when
        var summary = scoreboard.summary();

        // then
        assertThat(summary).isUnmodifiable();
    }

    @Test
    void shouldKeepScoreboardsIndependent() {
        // given
        var other = new Scoreboard();

        // when
        var onThisBoard = scoreboard.start(MEXICO_CANADA);
        var onOtherBoard = other.start(MEXICO_CANADA);

        // then
        assertThat(scoreboard.summary()).containsExactly(onThisBoard);
        assertThat(other.summary()).containsExactly(onOtherBoard);
    }

    private MatchDto play(StartMatch match, int homeScore, int awayScore) {
        var id = scoreboard.start(match).id();
        return scoreboard.update(new UpdateScore(id, homeScore, awayScore));
    }

    private static String line(MatchDto match) {
        return "%s %d - %s %d".formatted(match.homeTeam(), match.homeScore(), match.awayTeam(), match.awayScore());
    }
}
