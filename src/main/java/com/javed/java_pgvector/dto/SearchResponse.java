package com.javed.java_pgvector.dto;

import java.util.List;

public record SearchResponse(
        String query, int topK, int count, List<SearchHit> results
) {
}
