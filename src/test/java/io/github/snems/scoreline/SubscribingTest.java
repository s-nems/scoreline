package io.github.snems.scoreline;

import static io.github.snems.scoreline.SampleMatches.CANADA;
import static io.github.snems.scoreline.SampleMatches.MEXICO;
import static io.github.snems.scoreline.SampleMatches.MEXICO_CANADA;
import static io.github.snems.scoreline.SampleMatches.SPAIN_BRAZIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.github.snems.scoreline.dto.MatchDto;
import io.github.snems.scoreline.dto.MatchId;
import io.github.snems.scoreline.dto.UpdateScore;
import io.github.snems.scoreline.event.MatchFinished;
import io.github.snems.scoreline.event.MatchStarted;
import io.github.snems.scoreline.event.ScoreUpdated;
import io.github.snems.scoreline.event.ScoreboardEvent;
import io.github.snems.scoreline.event.Subscription;
import io.github.snems.scoreline.exception.MatchNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class SubscribingTest {

    private final Scoreboard scoreboard = new Scoreboard();

    private final List<ScoreboardEvent> events = new ArrayList<>();

    @Test
    void shouldTellWhenMatchStarted() {
        // given
        scoreboard.subscribe(events::add);

        // when
        var id = scoreboard.start(MEXICO_CANADA).id();

        // then
        assertThat(events).containsExactly(new MatchStarted(new MatchDto(id, MEXICO, CANADA, 0, 0)));
    }

    @Test
    void shouldTellWhenScoreUpdated() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.subscribe(events::add);

        // when
        scoreboard.update(new UpdateScore(id, 2, 1));

        // then
        assertThat(events).containsExactly(new ScoreUpdated(new MatchDto(id, MEXICO, CANADA, 2, 1)));
    }

    @Test
    void shouldTellWhenScoreUpdatedToSameScore() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.update(new UpdateScore(id, 2, 1));
        scoreboard.subscribe(events::add);

        // when
        scoreboard.update(new UpdateScore(id, 2, 1));

        // then
        assertThat(events).containsExactly(new ScoreUpdated(new MatchDto(id, MEXICO, CANADA, 2, 1)));
    }

    @Test
    void shouldTellWhenMatchFinished() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        scoreboard.update(new UpdateScore(id, 2, 1));
        scoreboard.subscribe(events::add);

        // when
        scoreboard.finish(id);

        // then
        assertThat(events).containsExactly(new MatchFinished(new MatchDto(id, MEXICO, CANADA, 2, 1)));
    }

    @Test
    void shouldDeliverNothingForRejectedRequest() {
        // given
        var unknownId = new MatchId(UUID.randomUUID());
        scoreboard.subscribe(events::add);

        // when
        var thrown = catchThrowable(() -> scoreboard.update(new UpdateScore(unknownId, 1, 0)));

        // then
        assertThat(thrown).isInstanceOf(MatchNotFoundException.class);
        assertThat(events).isEmpty();
    }

    @Test
    void shouldDeliverNothingAfterCancel() {
        // given
        var subscription = scoreboard.subscribe(events::add);
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);

        // when
        subscription.cancel();
        scoreboard.start(SPAIN_BRAZIL);

        // then
        assertThat(events).containsExactly(new MatchStarted(mexicoCanada));
    }

    @Test
    void shouldIgnoreSecondCancel() {
        // given
        var subscription = scoreboard.subscribe(events::add);
        subscription.cancel();

        // when
        var thrown = catchThrowable(subscription::cancel);

        // then
        assertThat(thrown).isNull();
    }

    @Test
    void shouldKeepOtherSubscriptionOfSameListener() {
        // given
        Consumer<ScoreboardEvent> listener = events::add;
        var subscription = scoreboard.subscribe(listener);
        scoreboard.subscribe(listener);

        // when
        subscription.cancel();
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);

        // then
        assertThat(events).containsExactly(new MatchStarted(mexicoCanada));
    }

    @Test
    void shouldLetListenerCancelItsOwnSubscription() {
        // given
        var subscription = new AtomicReference<Subscription>();
        subscription.set(scoreboard.subscribe(event -> {
            events.add(event);
            subscription.get().cancel();
        }));

        // when
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        scoreboard.start(SPAIN_BRAZIL);

        // then
        assertThat(events).containsExactly(new MatchStarted(mexicoCanada));
    }

    @Test
    void shouldIsolateFailingListener() {
        // given
        scoreboard.subscribe(event -> {
            throw new RuntimeException("listener failed");
        });
        scoreboard.subscribe(events::add);

        // when
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);

        // then
        assertThat(events).containsExactly(new MatchStarted(mexicoCanada));
        assertThat(scoreboard.summary()).containsExactly(mexicoCanada);
    }

    @Test
    void shouldLetErrorFromListenerReachCaller() {
        // given
        var failure = new Error("listener failed");
        scoreboard.subscribe(event -> {
            throw failure;
        });

        // when
        var thrown = catchThrowable(() -> scoreboard.start(MEXICO_CANADA));

        // then
        assertThat(thrown).isSameAs(failure);
        assertThat(scoreboard.summary()).hasSize(1);
    }

    @Test
    void shouldRejectChangeFromInsideListener() {
        // given
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);
        var thrown = new ArrayList<Throwable>();
        scoreboard.subscribe(event -> {
            thrown.add(catchThrowable(() -> scoreboard.start(SPAIN_BRAZIL)));
            thrown.add(catchThrowable(() -> scoreboard.update(new UpdateScore(mexicoCanada.id(), 9, 9))));
            thrown.add(catchThrowable(() -> scoreboard.finish(mexicoCanada.id())));
        });

        // when
        var updated = scoreboard.update(new UpdateScore(mexicoCanada.id(), 1, 0));

        // then
        assertThat(thrown).hasSize(3).allSatisfy(rejection -> assertThat(rejection)
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("listener"));
        assertThat(scoreboard.summary()).containsExactly(updated);
    }

    @Test
    void shouldShowChangeToListenerReadingSummary() {
        // given
        var seen = new ArrayList<List<MatchDto>>();
        scoreboard.subscribe(event -> seen.add(scoreboard.summary()));

        // when
        var mexicoCanada = scoreboard.start(MEXICO_CANADA);

        // then
        assertThat(seen).containsExactly(List.of(mexicoCanada));
    }

    @Test
    void shouldShowUpdateToListenerReadingSummary() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        var seen = new ArrayList<List<MatchDto>>();
        scoreboard.subscribe(event -> seen.add(scoreboard.summary()));

        // when
        var updated = scoreboard.update(new UpdateScore(id, 2, 1));

        // then
        assertThat(seen).containsExactly(List.of(updated));
    }

    @Test
    void shouldShowFinishToListenerReadingSummary() {
        // given
        var id = scoreboard.start(MEXICO_CANADA).id();
        var spainBrazil = scoreboard.start(SPAIN_BRAZIL);
        var seen = new ArrayList<List<MatchDto>>();
        scoreboard.subscribe(event -> seen.add(scoreboard.summary()));

        // when
        scoreboard.finish(id);

        // then
        assertThat(seen).containsExactly(List.of(spainBrazil));
    }

    @Test
    void shouldRejectNullListener() {
        // when
        var thrown = catchThrowable(() -> scoreboard.subscribe(null));

        // then
        assertThat(thrown).isInstanceOf(NullPointerException.class);
    }
}
