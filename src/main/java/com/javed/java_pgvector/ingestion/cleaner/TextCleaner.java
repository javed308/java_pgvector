package com.javed.java_pgvector.ingestion.cleaner;

import com.javed.java_pgvector.dto.CleanedDocument;
import com.javed.java_pgvector.dto.StructuredDocument;

public interface TextCleaner {
    CleanedDocument clean(StructuredDocument document);
}
