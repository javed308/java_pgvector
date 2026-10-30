package com.javed.java_pgvector.ingestion.parser;

import com.javed.java_pgvector.dto.ParsedDocument;

import java.io.IOException;
import java.io.InputStream;

public interface DocumentParser {

    ParsedDocument parse(InputStream input, String fileName) throws IOException;
}
