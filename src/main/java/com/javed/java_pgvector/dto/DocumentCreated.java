package com.javed.java_pgvector.dto;

import java.util.UUID;

public record DocumentCreated(
        UUID id, int dimensions
) {
}
