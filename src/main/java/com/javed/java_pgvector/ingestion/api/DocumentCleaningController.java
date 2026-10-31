package com.javed.java_pgvector.ingestion.api;

import com.javed.java_pgvector.ingestion.cleaner.TextCleaner;
import com.javed.java_pgvector.dto.CleanedDocument;
import com.javed.java_pgvector.ingestion.parser.DocumentParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@RestController
public class DocumentCleaningController {
    private final DocumentParser parser;
    private final TextCleaner cleaner;
    public DocumentCleaningController(DocumentParser parser, TextCleaner cleaner) {
        this.parser = parser;
        this.cleaner = cleaner;
    }
    @PostMapping(value = "/api/documents/clean", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public CleanedDocument clean(@RequestParam("file") MultipartFile file) throws IOException {
        try (InputStream input = file.getInputStream()) {
            return cleaner.clean(parser.parseStructured(input, file.getOriginalFilename()));
        }
    }
}
