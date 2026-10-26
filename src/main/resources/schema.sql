
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS public.document_chunks (
    id UUID PRIMARY KEY,
    content TEXT NOT NULL,
    embedding VECTOR(768) NOT NULL
);