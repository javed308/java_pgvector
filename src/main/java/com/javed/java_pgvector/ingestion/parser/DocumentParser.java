package com.javed.java_pgvector.ingestion.parser;

import com.javed.java_pgvector.dto.ParsedDocument;
import com.javed.java_pgvector.dto.StructuredDocument;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentParser {

    ParsedDocument parse(InputStream input, String fileName) throws IOException;

    /** Parsers without structural support can safely fall back to plain text. */
    default StructuredDocument parseStructured(InputStream input, String fileName) throws IOException {
        return StructuredDocument.plain(parse(input, fileName));
    }
}
