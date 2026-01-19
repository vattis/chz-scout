-- pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- stream_embedding 테이블 생성 (프로덕션과 동일한 스키마)
CREATE TABLE IF NOT EXISTS stream_embedding (
    channel_id VARCHAR(100) PRIMARY KEY,
    embedding_text TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 벡터 유사도 검색을 위한 IVFFlat 인덱스
-- 테스트 데이터가 적으므로 lists=10으로 설정 (프로덕션에서는 100 권장)
CREATE INDEX IF NOT EXISTS idx_stream_embedding_vector
ON stream_embedding USING ivfflat (embedding vector_cosine_ops) WITH (lists = 10);
