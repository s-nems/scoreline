package io.github.snems.scoreline;

/** The goals of both teams in a match. */
record Score(int home, int away) {

    static final Score GOALLESS = new Score(0, 0);

    Score {
        if (home < 0) {
            throw new IllegalArgumentException("home score must not be negative: %d".formatted(home));
        }
        if (away < 0) {
            throw new IllegalArgumentException("away score must not be negative: %d".formatted(away));
        }
        if (home > Integer.MAX_VALUE - away) {
            throw new IllegalArgumentException(
                    "total score must fit in an int: home %d, away %d".formatted(home, away));
        }
    }

    int total() {
        return home + away;
    }
}
