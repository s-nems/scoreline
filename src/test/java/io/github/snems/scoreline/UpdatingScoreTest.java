package io.github.snems.scoreline;

import static io.github.snems.scoreline.SampleMatches.BRAZIL;
import static io.github.snems.scoreline.SampleMatches.CANADA;
import static io.github.snems.scoreline.SampleMatches.GERMANY_FRANCE;
import static io.github.snems.scoreline.SampleMatches.MEXICO;
import static io.github.snems.scoreline.SampleMatches.MEXICO_CANADA;
import static io.github.snems.scoreline.SampleMatches.SPAIN;
import static io.github.snems.scoreline.SampleMatches.SPAIN_BRAZIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import io.github.snems.scoreline.dto.UpdateScore;
import io.github.snems.scoreline.exception.MatchNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class UpdatingScoreTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void shouldReplaceScore() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.update(new UpdateScore(id, 1, 0));

        // when
        var updated = scoreboard.update(new UpdateScore(id, 3, 2));

        // then
        assertThat(updated).isEqualTo(new MatchDto(id, MEXICO, CANADA, 3, 2));
        assertThat(scoreboard.summary()).containsExactly(updated);
    }

    @Test
    void shouldUpdateOnlyMatchWithGivenId() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        var id = scoreboard.start(SPAIN_BRAZIL).id();
        var germanyFrance = scoreboard.start(GERMANY_FRANCE);

        // when
        var spainBrazil = scoreboard.update(new UpdateScore(id, 1, 0));

        // then
        assertThat(spainBrazil).isEqualTo(new MatchDto(id, SPAIN, BRAZIL, 1, 0));
        assertThat(scoreboard.summary()).containsExactly(spainBrazil, germanyFrance, mexicoCanada);
    }

    @Test
    void shouldRejectNullCommand() {
        // when
        var thrown = catchThrowable(() -> scoreboard.update(null));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullMatchId() {
        // when
        var thrown = catchThrowable(() -> new UpdateScore(null, 1, 0));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectMatchIdWithoutValue() {
        // when
        var thrown = catchThrowable(() -> new MatchId(null));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvSource({"-1, 0", "0, -1"})
    void shouldRejectNegativeScore(int home, int away) {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);

        // when
        var thrown = catchThrowable(() -> scoreboard.update(new UpdateScore(mexicoCanada.id(), home, away)));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("negative");
        assertThat(scoreboard.summary()).containsExactly(mexicoCanada);
    }

    @Test
    void shouldAcceptTotalOfExactlyMaxInt() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();

        // when
        var updated = scoreboard.update(new UpdateScore(id, Integer.MAX_VALUE, 0));

        // then
        assertThat(updated).isEqualTo(new MatchDto(id, MEXICO, CANADA, Integer.MAX_VALUE, 0));
    }

    @Test
    void shouldRejectTotalAboveInt() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);

        // when
        var thrown = catchThrowable(
                () -> scoreboard.update(new UpdateScore(mexicoCanada.id(), Integer.MAX_VALUE, 1)));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("total");
        assertThat(scoreboard.summary()).containsExactly(mexicoCanada);
    }

    @Test
    void shouldRejectUnknownId() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        var unknownId = new MatchId(UUID.randomUUID());

        // when
        var thrown = catchThrowable(() -> scoreboard.update(new UpdateScore(unknownId, 7, 7)));

        // then
        assertThat(thrown)
                .hasMessageContaining(unknownId.value().toString())
                .isInstanceOfSatisfying(MatchNotFoundException.class,
                        rejection -> assertThat(rejection.matchId()).isEqualTo(unknownId));
        assertThat(scoreboard.summary()).containsExactly(mexicoCanada);
    }
}
