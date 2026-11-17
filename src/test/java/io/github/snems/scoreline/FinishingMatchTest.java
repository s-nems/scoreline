package io.github.snems.scoreline;

import static io.github.snems.scoreline.SampleMatches.CANADA;
import static io.github.snems.scoreline.SampleMatches.GERMANY_FRANCE;
import static io.github.snems.scoreline.SampleMatches.MEXICO;
import static io.github.snems.scoreline.SampleMatches.MEXICO_CANADA;
import static io.github.snems.scoreline.SampleMatches.SPAIN_BRAZIL;
import static io.github.snems.scoreline.SampleMatches.URUGUAY_ITALY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.UpdateScore;
import io.github.snems.scoreline.exception.MatchNotFoundException;
import org.junit.jupiter.api.Test;

class FinishingMatchTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void shouldReturnFinalScore() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.update(new UpdateScore(id, 3, 2));

        // when
        var finished = scoreboard.finish(id);

        // then
        assertThat(finished).isEqualTo(new MatchDto(id, MEXICO, CANADA, 3, 2));
        assertThat(scoreboard.summary()).isEmpty();
    }

    @Test
    void shouldFinishOnlyMatchWithGivenId() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        var spainBrazil = scoreboard.start(SPAIN_BRAZIL);
        var germanyFrance = scoreboard.start(GERMANY_FRANCE);

        // when
        var finished = scoreboard.finish(spainBrazil.id());

        // then
        assertThat(finished).isEqualTo(spainBrazil);
        assertThat(scoreboard.summary()).containsExactly(germanyFrance, mexicoCanada);
    }

    @Test
    void shouldRejectNullId() {
        // when
        var thrown = catchThrowable(() -> scoreboard.finish(null));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectFinishingTwice() {
        // given
        var finishedId = scoreboard.start(MEXICO_CANADA).id();
        var spainBrazil = scoreboard.start(SPAIN_BRAZIL);
        scoreboard.finish(finishedId);

        // when
        var thrown = catchThrowable(() -> scoreboard.finish(finishedId));

        // then
        assertThat(thrown)
                .hasMessageContaining(finishedId.value().toString())
                .isInstanceOfSatisfying(MatchNotFoundException.class,
                        rejection -> assertThat(rejection.matchId()).isEqualTo(finishedId));
        assertThat(scoreboard.summary()).containsExactly(spainBrazil);
    }

    @Test
    void shouldRejectUpdateOfFinishedMatch() {
        // given
        var finishedId = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.finish(finishedId);
        var rematch = scoreboard.start(MEXICO_CANADA);

        // when
        var thrown = catchThrowable(() -> scoreboard.update(new UpdateScore(finishedId, 1, 0)));

        // then
        assertThat(thrown).isInstanceOf(MatchNotFoundException.class);
        assertThat(scoreboard.summary()).containsExactly(rematch);
    }

    @Test
    void shouldStartRematchAsNewMatch() {
        // given
        var finishedId = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.update(new UpdateScore(finishedId, 2, 1));
        scoreboard.finish(finishedId);

        // when
        var rematch = scoreboard.start(MEXICO_CANADA);

        // then
        assertThat(rematch.id()).isNotEqualTo(finishedId);
        assertThat(rematch).isEqualTo(new MatchDto(rematch.id(), MEXICO, CANADA, 0, 0));
    }

    @Test
    void shouldTreatStartAfterFinishAsMostRecent() {
        // given
        var finishedId = scoreboard.start(MEXICO_CANADA).id();
        var spainBrazil = scoreboard.start(SPAIN_BRAZIL);
        var germanyFrance = scoreboard.start(GERMANY_FRANCE);
        scoreboard.finish(finishedId);

        // when
        var uruguayItaly = scoreboard.start(URUGUAY_ITALY);

        // then
        assertThat(scoreboard.summary()).containsExactly(uruguayItaly, germanyFrance, spainBrazil);
    }
}
