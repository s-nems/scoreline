package io.github.snems.scoreline;

import static io.github.snems.scoreline.SampleMatches.MEXICO;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.StartMatch;
import io.github.snems.scoreline.dto.UpdateScore;
import io.github.snems.scoreline.exception.TeamAlreadyPlayingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

    private static final int THREADS = 8;
    private static final int MATCHES_PER_THREAD = 100;
    private static final int CONTENDERS = 32;
    private static final int FINISHERS = 32;
    private static final int LAST_SCORE = 500;
    private static final int TIMEOUT_SECONDS = 10;

    private final Scoreboard scoreboard = new Scoreboard();

    @Test
    void shouldKeepEveryMatchStartedAtTheSameTime() throws Exception {
        // given
        var starters = IntStream.range(0, THREADS).mapToObj(this::starterOfManyMatches).toList();

        // when
        var started = runTogether(starters).stream().flatMap(List::stream).toList();

        // then
        assertThat(started).hasSize(THREADS * MATCHES_PER_THREAD);
        assertThat(scoreboard.summary()).containsExactlyInAnyOrderElementsOf(started);
    }

    @Test
    void shouldStartOnlyOneMatchOfTeamUnderContention() throws Exception {
        // given
        var contenders = IntStream.range(0, CONTENDERS).mapToObj(this::starterOfMexicoMatch).toList();

        // when
        var thrown = runTogether(contenders);

        // then
        assertThat(thrown).filteredOn(Objects::isNull).hasSize(1);
        assertThat(thrown)
                .filteredOn(Objects::nonNull)
                .hasSize(CONTENDERS - 1)
                .hasOnlyElementsOfType(TeamAlreadyPlayingException.class);
        assertThat(scoreboard.summary()).hasSize(1);
    }

    @Test
    void shouldKeepEveryUpdateAndFinishMadeAtTheSameTime() throws Exception {
        // given
        var updating = startMatches(0, THREADS);
        var finishing = startMatches(THREADS, THREADS + FINISHERS);
        var updaters = updating.stream().map(this::updaterOfScore);
        var finishers = finishing.stream().map(this::finisherOfMatch);

        // when
        var results = runTogether(Stream.concat(updaters, finishers).toList());

        // then
        var lastUpdates = updating.stream().map(ConcurrencyTest::withLastScore).toList();
        var returned = Stream.concat(lastUpdates.stream(), finishing.stream()).toList();
        assertThat(results).containsExactlyElementsOf(returned);
        assertThat(scoreboard.summary()).containsExactlyInAnyOrderElementsOf(lastUpdates);
    }

    private Callable<List<MatchDto>> starterOfManyMatches(int thread) {
        return () -> IntStream.range(0, MATCHES_PER_THREAD)
                .mapToObj(match -> new StartMatch("Home %d-%d".formatted(thread, match),
                        "Away %d-%d".formatted(thread, match)))
                .map(scoreboard::start)
                .toList();
    }

    private Callable<MatchDto> updaterOfScore(MatchDto match) {
        return () -> {
            var updated = match;
            for (var goals = 1; goals <= LAST_SCORE; goals++) {
                updated = scoreboard.update(new UpdateScore(match.id(), goals, 0));
            }
            return updated;
        };
    }

    private Callable<MatchDto> finisherOfMatch(MatchDto match) {
        return () -> scoreboard.finish(match.id());
    }

    private List<MatchDto> startMatches(int from, int to) {
        return IntStream.range(from, to).mapToObj(this::startMatch).toList();
    }

    private MatchDto startMatch(int number) {
        return scoreboard.start(new StartMatch("Home %d".formatted(number), "Away %d".formatted(number)));
    }

    private static MatchDto withLastScore(MatchDto match) {
        return new MatchDto(match.id(), match.homeTeam(), match.awayTeam(), LAST_SCORE, 0);
    }

    private Callable<Throwable> starterOfMexicoMatch(int contender) {
        return () -> catchThrowable(() -> scoreboard.start(new StartMatch(MEXICO, "Away %d".formatted(contender))));
    }

    /** Runs each task on its own thread, all released at once, and fails instead of waiting forever. */
    private static <T> List<T> runTogether(List<Callable<T>> tasks) throws Exception {
        var ready = new CountDownLatch(tasks.size());
        var go = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(tasks.size(), Thread.ofPlatform().daemon().factory());
        try {
            var futures = tasks.stream().map(task -> executor.submit(() -> {
                ready.countDown();
                go.await();
                return task.call();
            })).toList();
            assertThat(ready.await(TIMEOUT_SECONDS, SECONDS)).isTrue();
            go.countDown();
            var results = new ArrayList<T>();
            for (var future : futures) {
                results.add(future.get(TIMEOUT_SECONDS, SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }
}
