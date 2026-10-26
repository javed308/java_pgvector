package com.javed.java_pgvector.dto;

import java.util.UUID;

public record SearchHit(UUID id, String content, double distance, double similarity) {
}
