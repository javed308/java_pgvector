package com.javed.java_pgvector.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank(message = "content must not be blank")
        @Size(max = 4000, message = "content must be at most 4000 characters; send a shorter chunk")
        String content
) {
}
