package io.github.snems.scoreline;

/** The goals of both teams in a match. */
record Score(int home, int away) {

    static final Score GOALLESS = new Score(0, 0);
}
