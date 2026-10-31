package com.javed.java_pgvector.dto;

import java.util.List;
import java.util.Map;

public record CleanedDocument(String fileName, String contentType, String extractedText,
                              String cleanedText, Map<String, List<String>> metadata,
                              CleaningReport report) {
    public record CleaningReport(String cleanerVersion, int inputCharacters, int outputCharacters,
                                 int recognizedHeadings, int pageCount,
                                 Map<String, Integer> removalsByReason,
                                 List<RemovedText> removalPreview, boolean previewTruncated,
                                 List<String> warnings) {
        public CleaningReport {
            removalsByReason = Map.copyOf(removalsByReason);
            removalPreview = List.copyOf(removalPreview);
            warnings = List.copyOf(warnings);
        }
    }
    /** The preview is bounded; extractedText remains available for comparison. */
    public record RemovedText(String reason, Integer pageNumber, String text) {}
}
