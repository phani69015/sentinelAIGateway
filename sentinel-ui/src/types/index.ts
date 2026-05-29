export interface QueryRequest {
  query: string;
  context?: {
    customerSegment?: string;
    productCategory?: string;
    knowledgeBase?: string[];
  };
  options?: {
    strictMode?: boolean;
    timeoutMs?: number;
  };
}

export interface QueryResponse {
  response: string;
  auditId: string;
  verdict: 'PASS' | 'WARN' | 'BLOCK';
  confidence: number;
  metadata: {
    respondersAgreed: boolean;
    complianceChecks: number;
    violationsFound: number;
    latencyMs: number;
  };
}

export interface AuditRecord {
  id: string;
  timestamp: string;
  query: string;
  responseA: {
    provider: string;
    text: string;
    model: string;
  };
  responseB: {
    provider: string;
    text: string;
    model: string;
  };
  auditDecision: {
    verdict: 'PASS' | 'WARN' | 'BLOCK';
    confidence: number;
    consistencyScore: number;
    reasoning: string;
  };
  deliveredResponse: string;
  verdict: 'PASS' | 'WARN' | 'BLOCK';
  latencyMs: number;
}

export interface AuditStats {
  total: number;
  passed: number;
  warned: number;
  blocked: number;
}
