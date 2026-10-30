package com.javed.java_pgvector.ingestion.api;

import com.javed.java_pgvector.dto.ParsedDocument;
import com.javed.java_pgvector.ingestion.parser.DocumentParser;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;


@RestController
public class DocumentParsingController {

    private final DocumentParser documentParser;

    public DocumentParsingController(DocumentParser documentParser) {
        this.documentParser = documentParser;
    }

    @PostMapping(value = "/api/documents/parse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ParsedDocument parse(@RequestParam("file") MultipartFile file)
            throws IOException {
        try (InputStream input = file.getInputStream()) {
            return documentParser.parse(input, file.getOriginalFilename());
        }
    }
}
