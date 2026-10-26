package com.javed.java_pgvector.service;

import com.javed.java_pgvector.dto.DocumentCreated;
import com.javed.java_pgvector.dto.SearchHit;
import com.javed.java_pgvector.dto.SearchResponse;
import com.javed.java_pgvector.repository.DocumentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final EmbeddingService embeddings;
    private final DocumentRepository repository;

    public DocumentService(EmbeddingService embeddings, DocumentRepository repository) {
        this.embeddings = embeddings;
        this.repository = repository;
    }

    public DocumentCreated create(String content) {
        String text = content.strip();
        // Call Ollama BEFORE the database write. No database transaction is held open here.
        float[] vector = embeddings.embedDocument(text);
        UUID id = UUID.randomUUID();
        // Save plain text. The model-specific prefix is not stored in content.
        repository.insert(id, text, vector);
        return new DocumentCreated(id, vector.length);
    }

    public SearchResponse search(String query, int topK) {
        String text = query.strip();
        float[] queryVector = embeddings.embedQuery(text);
        List<SearchHit> hits = repository.search(queryVector, topK);
        return new SearchResponse(text, topK, hits.size(), hits);
    }
}
