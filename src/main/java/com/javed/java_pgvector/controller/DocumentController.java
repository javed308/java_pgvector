package com.javed.java_pgvector.controller;

import com.javed.java_pgvector.dto.CreateDocumentRequest;
import com.javed.java_pgvector.dto.DocumentCreated;
import com.javed.java_pgvector.dto.SearchResponse;
import com.javed.java_pgvector.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentCreated create(@Valid @RequestBody CreateDocumentRequest request) {
        return service.create(request.content());
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(name = "q") @NotBlank @Size(max = 1000) String query,
            @RequestParam(name = "topK", defaultValue = "5") @Min(1) @Max(50) int topK) {
        return service.search(query, topK);
    }

}
