package com.javed.java_pgvector.repository;

import com.javed.java_pgvector.dto.SearchHit;
import com.javed.java_pgvector.helper.VectorCodec;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class DocumentRepository {

    private final JdbcClient jdbcClient;

    public DocumentRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public void insert(UUID id, String content, float[] embedding) {

        jdbcClient.sql("""
                INSERT INTO public.document_chunks (id, content, embedding)
                VALUES (:id, :content, CAST(:embedding AS vector))
                """)
                .param("id", id)
                .param("content", content)
                .param("embedding", VectorCodec.toLiteral(embedding))
                .update();

    }

    public List<SearchHit> search(float[] queryEmbedding, int topK) {
        return jdbcClient.sql("""
                SELECT id, content,
                       embedding <=> CAST(:queryEmbedding AS vector) AS distance
                FROM public.document_chunks
                WHERE content IS NOT NULL
                  AND embedding IS NOT NULL
                  AND vector_norm(embedding) > 0
                ORDER BY embedding <=> CAST(:queryEmbedding AS vector) ASC
                LIMIT :topK
                """)
                .param("queryEmbedding", VectorCodec.toLiteral(queryEmbedding))
                .param("topK", topK)
                .query((rs, rowNum) -> {
                    double distance = rs.getDouble("distance");
                    return new SearchHit(rs.getObject("id", UUID.class),
                            rs.getString("content"), distance, 1.0 - distance);
                })
                .list();
    }
}
