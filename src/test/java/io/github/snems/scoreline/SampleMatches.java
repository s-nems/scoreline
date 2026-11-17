package io.github.snems.scoreline;

import io.github.snems.scoreline.dto.StartMatch;

final class SampleMatches {

    static final String MEXICO = "Mexico";
    static final String CANADA = "Canada";
    static final String SPAIN = "Spain";
    static final String BRAZIL = "Brazil";
    static final String GERMANY = "Germany";
    static final String FRANCE = "France";
    static final String URUGUAY = "Uruguay";
    static final String ITALY = "Italy";
    static final String ARGENTINA = "Argentina";
    static final String AUSTRALIA = "Australia";

    static final StartMatch MEXICO_CANADA = new StartMatch(MEXICO, CANADA);
    static final StartMatch SPAIN_BRAZIL = new StartMatch(SPAIN, BRAZIL);
    static final StartMatch GERMANY_FRANCE = new StartMatch(GERMANY, FRANCE);
    static final StartMatch URUGUAY_ITALY = new StartMatch(URUGUAY, ITALY);
    static final StartMatch ARGENTINA_AUSTRALIA = new StartMatch(ARGENTINA, AUSTRALIA);

    private SampleMatches() {
    }
}
