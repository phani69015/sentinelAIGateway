CREATE TABLE audit_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    query TEXT NOT NULL,
    context_data TEXT,
    response_a TEXT NOT NULL,
    response_a_provider VARCHAR(20) NOT NULL,
    response_a_model VARCHAR(100),
    response_b TEXT NOT NULL,
    response_b_provider VARCHAR(20) NOT NULL,
    response_b_model VARCHAR(100),
    verdict VARCHAR(10) NOT NULL,
    confidence_score DOUBLE PRECISION NOT NULL,
    consistency_score DOUBLE PRECISION NOT NULL,
    audit_reasoning TEXT NOT NULL,
    delivered_response TEXT,
    latency_ms BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Immutable: no UPDATE or DELETE allowed via application
-- Index for querying by verdict and time range
CREATE INDEX idx_audit_records_verdict ON audit_records (verdict);
CREATE INDEX idx_audit_records_created_at ON audit_records (created_at DESC);
CREATE INDEX idx_audit_records_verdict_created ON audit_records (verdict, created_at DESC);
