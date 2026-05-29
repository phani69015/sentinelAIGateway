CREATE TABLE compliance_violations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    audit_record_id UUID NOT NULL REFERENCES audit_records(id) ON DELETE CASCADE,
    violation_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    severity DOUBLE PRECISION NOT NULL,
    offending_text TEXT,
    source_provider VARCHAR(20)
);

CREATE INDEX idx_violations_audit_record ON compliance_violations (audit_record_id);
CREATE INDEX idx_violations_type ON compliance_violations (violation_type);
