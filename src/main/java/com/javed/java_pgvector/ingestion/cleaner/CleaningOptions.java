package com.javed.java_pgvector.ingestion.cleaner;

import java.util.Set;

public record CleaningOptions(boolean removeMarkedBoilerplate,
                              boolean detectRepeatedPageMargins,
                              Set<String> knownMarginLines) {
    public CleaningOptions {
        knownMarginLines = Set.copyOf(knownMarginLines);
    }
    public static CleaningOptions defaults() {
        return new CleaningOptions(true, false, Set.of());
    }
}