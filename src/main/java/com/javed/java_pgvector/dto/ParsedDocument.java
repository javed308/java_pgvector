package com.javed.java_pgvector.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ParsedDocument(
        String fileName,
        String contentType,
        String text,
        Map<String, List<String>> metadata
) {
    public ParsedDocument {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(contentType, "contentType");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(metadata, "metadata");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        metadata.forEach((key, values) -> copy.put(key, List.copyOf(values)));
        metadata = Map.copyOf(copy);
    }
}
