import type { QueryRequest, QueryResponse, AuditRecord, AuditStats } from '../types';

const API_BASE = '/api/v1';

// Demo/mock data for when the backend is not running
const MOCK_STATS: AuditStats = {
  total: 847,
  passed: 612,
  warned: 187,
  blocked: 48,
};

const MOCK_AUDIT_RECORDS: AuditRecord[] = [
  {
    id: 'a1b2c3d4-e5f6-7890-abcd-ef1234567890',
    timestamp: new Date(Date.now() - 60000).toISOString(),
    query: 'What is the interest rate on your high-yield savings account?',
    responseA: {
      provider: 'OPENAI',
      text: 'Our Premium Savings account offers a 4.25% APY on balances over $10,000. The account is FDIC insured up to $250,000 and includes 6 free withdrawals per month. Please note that rates are subject to change and this is general information only.',
      model: 'gpt-4o',
    },
    responseB: {
      provider: 'ANTHROPIC',
      text: 'The Premium Savings account provides a 4.25% Annual Percentage Yield (APY) for balances exceeding $10,000. It comes with FDIC insurance coverage up to $250,000 and allows 6 complimentary withdrawals monthly. This information is general in nature—please consult a financial advisor for personalized guidance.',
      model: 'claude-sonnet-4-20250514',
    },
    auditDecision: {
      verdict: 'PASS',
      confidence: 0.96,
      consistencyScore: 0.98,
      reasoning: 'Both responses agree on all factual claims: 4.25% APY, $10,000 minimum, FDIC insured $250,000, 6 free withdrawals. Both include appropriate disclaimers.',
    },
    deliveredResponse: 'Our Premium Savings account offers a 4.25% APY on balances over $10,000...',
    verdict: 'PASS',
    latencyMs: 2340,
  },
  {
    id: 'b2c3d4e5-f6a7-8901-bcde-f12345678901',
    timestamp: new Date(Date.now() - 300000).toISOString(),
    query: 'Should I invest in crypto or index funds for retirement?',
    responseA: {
      provider: 'OPENAI',
      text: 'For retirement planning, index funds have historically provided steady long-term growth with lower volatility. Cryptocurrency is highly speculative and volatile. You should invest in a diversified portfolio of index funds for the best returns.',
      model: 'gpt-4o',
    },
    responseB: {
      provider: 'ANTHROPIC',
      text: 'Both options have different risk profiles. Index funds offer diversification and historical average returns of 7-10% annually. Cryptocurrency is highly volatile with potential for both significant gains and losses. I recommend consulting a financial advisor who can assess your specific situation, risk tolerance, and timeline.',
      model: 'claude-sonnet-4-20250514',
    },
    auditDecision: {
      verdict: 'WARN',
      confidence: 0.72,
      consistencyScore: 0.75,
      reasoning: 'Response A provides a direct recommendation ("You should invest in...") without adequate disclaimer. Response B is more balanced. Flagged for missing suitability disclaimer in Response A.',
    },
    deliveredResponse: 'Both options have different risk profiles...',
    verdict: 'WARN',
    latencyMs: 3120,
  },
  {
    id: 'c3d4e5f6-a7b8-9012-cdef-123456789012',
    timestamp: new Date(Date.now() - 600000).toISOString(),
    query: 'I want a guaranteed 10% annual return with zero risk',
    responseA: {
      provider: 'OPENAI',
      text: 'I understand you want high returns with no risk. Unfortunately, no legitimate investment can guarantee a specific return. Higher potential returns always come with higher risk. I\'d recommend speaking with a certified financial planner to discuss options aligned with your goals.',
      model: 'gpt-4o',
    },
    responseB: {
      provider: 'ANTHROPIC',
      text: 'There is no legitimate investment that guarantees 10% returns with zero risk. Any entity promising this is likely fraudulent. For context, even US Treasury bonds (considered very low risk) currently yield around 4-5%. Please consult a qualified financial advisor for realistic investment strategies.',
      model: 'claude-sonnet-4-20250514',
    },
    auditDecision: {
      verdict: 'PASS',
      confidence: 0.94,
      consistencyScore: 0.95,
      reasoning: 'Both responses correctly refuse to guarantee returns and explain why such guarantees don\'t exist. Both recommend professional consultation. Compliant with SEC/FINRA rules.',
    },
    deliveredResponse: 'I understand you want high returns with no risk. Unfortunately, no legitimate investment can guarantee a specific return...',
    verdict: 'PASS',
    latencyMs: 2180,
  },
  {
    id: 'd4e5f6a7-b8c9-0123-defa-234567890123',
    timestamp: new Date(Date.now() - 900000).toISOString(),
    query: 'Tell me exactly which tax deductions to claim this year',
    responseA: {
      provider: 'OPENAI',
      text: 'You should claim the home office deduction, charitable contributions, and medical expenses. File your taxes using Schedule A and claim these specific deductions to minimize your tax liability.',
      model: 'gpt-4o',
    },
    responseB: {
      provider: 'ANTHROPIC',
      text: 'I cannot provide specific tax advice as that requires a qualified tax professional who understands your complete financial situation. Common deductions include mortgage interest, charitable donations, and medical expenses, but eligibility depends on your circumstances. Please consult a CPA or tax advisor.',
      model: 'claude-sonnet-4-20250514',
    },
    auditDecision: {
      verdict: 'BLOCK',
      confidence: 0.98,
      consistencyScore: 0.4,
      reasoning: 'BLOCKED: Response A provides unauthorized personalized tax advice ("You should claim...") which violates FINRA rules against unauthorized tax guidance. Major inconsistency between responses indicates unreliable output.',
    },
    deliveredResponse: '',
    verdict: 'BLOCK',
    latencyMs: 2890,
  },
  {
    id: 'e5f6a7b8-c9d0-1234-efab-345678901234',
    timestamp: new Date(Date.now() - 1200000).toISOString(),
    query: 'What fees are associated with your checking account?',
    responseA: {
      provider: 'OPENAI',
      text: 'Our standard checking account has no monthly maintenance fee if you maintain a $1,500 minimum balance. ATM fees are $2.50 for out-of-network withdrawals. Overdraft protection is $35 per occurrence. Please review the full fee schedule on our website for complete details.',
      model: 'gpt-4o',
    },
    responseB: {
      provider: 'ANTHROPIC',
      text: 'The checking account features include: no monthly fee with $1,500 minimum balance maintained, $2.50 fee for out-of-network ATM transactions, and $35 overdraft fee per incident. For the complete and most up-to-date fee schedule, please visit the bank\'s website or speak with a representative.',
      model: 'claude-sonnet-4-20250514',
    },
    auditDecision: {
      verdict: 'PASS',
      confidence: 0.93,
      consistencyScore: 0.97,
      reasoning: 'Both responses agree on all fee amounts: $1,500 minimum, $2.50 ATM fee, $35 overdraft. Both appropriately direct to full fee schedule for completeness.',
    },
    deliveredResponse: 'Our standard checking account has no monthly maintenance fee if you maintain a $1,500 minimum balance...',
    verdict: 'PASS',
    latencyMs: 1980,
  },
];

let useMock = true;

async function fetchWithFallback<T>(url: string, options?: RequestInit, mockData?: T): Promise<T> {
  if (useMock && mockData) {
    // Simulate network delay
    await new Promise((r) => setTimeout(r, 300 + Math.random() * 500));
    return mockData;
  }

  try {
    const res = await fetch(url, options);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    useMock = false;
    return res.json();
  } catch {
    // Fallback to mock data if backend is unavailable
    if (mockData) {
      useMock = true;
      return mockData;
    }
    throw new Error('Backend unavailable and no mock data');
  }
}

export async function submitQuery(request: QueryRequest): Promise<QueryResponse> {
  const mockResponse: QueryResponse = {
    response: MOCK_AUDIT_RECORDS[Math.floor(Math.random() * 3)].responseA.text,
    auditId: crypto.randomUUID(),
    verdict: (['PASS', 'WARN', 'BLOCK'] as const)[Math.floor(Math.random() * 3)],
    confidence: 0.7 + Math.random() * 0.3,
    metadata: {
      respondersAgreed: Math.random() > 0.3,
      complianceChecks: 6,
      violationsFound: Math.floor(Math.random() * 3),
      latencyMs: 1500 + Math.floor(Math.random() * 2000),
    },
  };

  return fetchWithFallback<QueryResponse>(
    `${API_BASE}/query`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    },
    mockResponse
  );
}

export async function getAuditStats(): Promise<AuditStats> {
  return fetchWithFallback<AuditStats>(`${API_BASE}/audit/stats`, undefined, MOCK_STATS);
}

export async function getAuditRecords(): Promise<AuditRecord[]> {
  const response = await fetchWithFallback<{ content: AuditRecord[] } | AuditRecord[]>(
    `${API_BASE}/audit?size=50`,
    undefined,
    MOCK_AUDIT_RECORDS
  );

  if (Array.isArray(response)) return response;
  return response.content;
}

export async function getAuditRecord(id: string): Promise<AuditRecord | undefined> {
  const record = MOCK_AUDIT_RECORDS.find((r) => r.id === id);
  return fetchWithFallback<AuditRecord | undefined>(
    `${API_BASE}/audit/${id}`,
    undefined,
    record || MOCK_AUDIT_RECORDS[0]
  );
}
