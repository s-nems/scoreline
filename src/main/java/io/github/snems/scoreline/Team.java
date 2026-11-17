package io.github.snems.scoreline;

import static java.util.Locale.ROOT;

/**
 * A football team. Compare teams with {@link #isSameTeamAs(Team)}, the generated {@code equals} compares the name
 * exactly.
 */
record Team(String name) {

    Team {
        name = name.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("team name must not be blank");
        }
    }

    boolean isSameTeamAs(Team other) {
        return name.toLowerCase(ROOT).equals(other.name.toLowerCase(ROOT));
    }
}
