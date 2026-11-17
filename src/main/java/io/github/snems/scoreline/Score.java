package io.github.snems.scoreline;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/** The goals of both teams in a match. */
record Score(int home, int away) {

    private static final Logger log = getLogger(Score.class);

    static final Score GOALLESS = new Score(0, 0);

    Score {
        if (home < 0) {
            log.warn("Rejected negative score: {} - {}", home, away);
            throw new IllegalArgumentException("home score must not be negative: %d".formatted(home));
        }
        if (away < 0) {
            log.warn("Rejected negative score: {} - {}", home, away);
            throw new IllegalArgumentException("away score must not be negative: %d".formatted(away));
        }
        if (home > Integer.MAX_VALUE - away) {
            log.warn("Rejected score whose total does not fit in an int: {} - {}", home, away);
            throw new IllegalArgumentException(
                    "total score must fit in an int: home %d, away %d".formatted(home, away));
        }
    }

    int total() {
        return home + away;
    }
}
