package io.github.snems.scoreline;

import static io.github.snems.scoreline.SampleMatches.CANADA;
import static io.github.snems.scoreline.SampleMatches.GERMANY;
import static io.github.snems.scoreline.SampleMatches.MEXICO;
import static io.github.snems.scoreline.SampleMatches.MEXICO_CANADA;
import static io.github.snems.scoreline.SampleMatches.SPAIN_BRAZIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.StartMatch;
import io.github.snems.scoreline.exception.TeamAlreadyPlayingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StartingMatchTest {

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void shouldStartGoalless() {
        // when
        var match = scoreboard.start(MEXICO_CANADA);

        // then
        assertThat(match).isEqualTo(new MatchDto(match.id(), MEXICO, CANADA, 0, 0));
        assertThat(scoreboard.summary()).containsExactly(match);
    }

    @Test
    void shouldRejectNullCommand() {
        // when
        var thrown = catchThrowable(() -> scoreboard.start(null));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullHomeTeam() {
        // when
        var thrown = catchThrowable(() -> new StartMatch(null, CANADA));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldRejectNullAwayTeam() {
        // when
        var thrown = catchThrowable(() -> new StartMatch(MEXICO, null));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void shouldRejectBlankName(String blank) {
        // when
        var thrown = catchThrowable(() -> scoreboard.start(new StartMatch(blank, CANADA)));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("blank");
    }

    @Test
    void shouldIgnoreWhitespaceAroundName() {
        // when
        var match = scoreboard.start(new StartMatch(" \tSouth  Korea\n", " Canada "));

        // then
        assertThat(match).isEqualTo(new MatchDto(match.id(), "South  Korea", CANADA, 0, 0));
    }

    @ParameterizedTest(name = "Mexico against {0}")
    @ValueSource(strings = {"Mexico", "MEXICO", " Mexico "})
    void shouldRejectTeamAgainstItself(String away) {
        // when
        var thrown = catchThrowable(() -> scoreboard.start(new StartMatch(MEXICO, away)));

        // then
        assertThat(thrown).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("different teams");
        assertThat(scoreboard.summary()).isEmpty();
    }

    @ParameterizedTest(name = "{0} against {1}")
    @CsvSource({
            "Mexico, Spain, Mexico",
            "Canada, Spain, Canada",
            "Spain, Mexico, Mexico",
            "Spain, Canada, Canada",
            "MEXICO, Spain, MEXICO"
    })
    void shouldRejectTeamAlreadyPlaying(String home, String away, String playing) {
        // given
        scoreboard.start(MEXICO_CANADA);

        // when
        var thrown = catchThrowable(() -> scoreboard.start(new StartMatch(home, away)));

        // then
        assertThat(thrown)
                .hasMessageContaining(playing)
                .isInstanceOfSatisfying(TeamAlreadyPlayingException.class,
                        rejection -> assertThat(rejection.team()).isEqualTo(playing));
    }

    @Test
    void shouldKeepBoardAfterRejectedStart() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        var spainBrazil = scoreboard.start(SPAIN_BRAZIL);

        // when
        var thrown = catchThrowable(() -> scoreboard.start(new StartMatch(GERMANY, MEXICO)));

        // then
        assertThat(thrown).isInstanceOf(TeamAlreadyPlayingException.class);
        assertThat(scoreboard.summary()).containsExactly(spainBrazil, mexicoCanada);
    }
}
