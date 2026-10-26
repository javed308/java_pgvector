package com.javed.java_pgvector.service;

import com.javed.java_pgvector.helper.VectorCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);
    private final EmbeddingModel model;
    private final int dimensions;

    public EmbeddingService(EmbeddingModel model,
                            @Value("${app.embedding-dimensions:768}") int dimensions) {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("app.embedding-dimensions must be positive");
        }
        this.model = model;
        this.dimensions = dimensions;
    }

    public float[] embedDocument(String content) {
        return embed("search_document: " + content);
    }

    public float[] embedQuery(String query) {
        return embed("search_query: " + query);
    }

    private float[] embed(String prefixedText) {
        final float[] vector;
        try {
            vector = model.embed(prefixedText);
        } catch (RuntimeException ex) {
            // Do not log the supplied text, vector, or a provider response body.
            log.warn("Ollama embedding request failed: {}", ex.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Ollama could not create an embedding. Check the server and model, "
                            + "and try a shorter text chunk.", ex);
        }
        try {
            VectorCodec.validate(vector, dimensions);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Invalid embedding returned by Ollama: " + ex.getMessage(), ex);
        }
        return vector;
    }
}
