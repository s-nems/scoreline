package io.github.snems.scoreline;

import static java.util.Locale.ROOT;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * A football team. Compare teams with {@link #isSameTeamAs(Team)}, the generated {@code equals} compares the name
 * exactly.
 */
record Team(String name) {

    private static final Logger log = getLogger(Team.class);

    Team {
        name = name.strip();
        if (name.isEmpty()) {
            log.warn("Rejected blank team name");
            throw new IllegalArgumentException("team name must not be blank");
        }
    }

    boolean isSameTeamAs(Team other) {
        return name.toLowerCase(ROOT).equals(other.name.toLowerCase(ROOT));
    }
}
