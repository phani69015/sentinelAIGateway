# Sentinel AI

A self-auditing LLM gateway that prevents hallucinated or non-compliant responses from reaching customers in regulated industries.

## Problem

Financial institutions cannot deploy LLMs for customer-facing use because:
- LLMs hallucinate rates, fees, and product details that don't exist
- A single wrong answer can trigger SEC/FINRA regulatory violations
- There's no built-in mechanism to detect when an LLM is "confidently wrong"
- Manual human review of every response doesn't scale

## What Sentinel AI Does

Sentinel AI sits between your application and LLM providers. Every query goes through a **triple-agent validation pipeline**:

```
Customer Query
      │
      ├──► LLM-A (OpenAI)     ──┐
      │                          ├──► Audit Agent ──► Validated Response
      └──► LLM-B (Anthropic)  ──┘        │
                                          ▼
                                 Consistency Check
                                 Hallucination Detection
                                 Compliance Scan (SEC/FINRA)
                                 Toxicity Filter
                                          │
                                    PASS / WARN / BLOCK
```

Two independent LLMs answer every query. A third Audit Agent cross-validates their outputs and only delivers the response if it passes all checks.

## How Context / Knowledge Base Works

Each query can include a `knowledgeBase` — verified facts from your internal systems (product rates, fees, terms, policies). This is how the system prevents hallucinations:

- LLMs are instructed to answer **only** from the provided context
- The Audit Agent verifies that claims in the response actually exist in the knowledge base
- If an LLM invents a rate or product feature not in the context, it's flagged and blocked

Without context, the system still works — it cross-checks the two LLMs against each other for consistency.

## API

### Submit a Query

```
POST /api/v1/query
```

```json
{
  "query": "What is the interest rate on your savings account?",
  "context": {
    "customerSegment": "retail",
    "productCategory": "savings",
    "knowledgeBase": [
      "Premium Savings: 4.25% APY on balances over $10,000",
      "FDIC insured up to $250,000",
      "6 free withdrawals per month"
    ]
  },
  "options": {
    "strictMode": false
  }
}
```

**Response:**

```json
{
  "response": "Our Premium Savings account offers 4.25% APY on balances over $10,000...",
  "auditId": "a1b2c3d4-...",
  "verdict": "PASS",
  "confidence": 0.95,
  "metadata": {
    "respondersAgreed": true,
    "complianceChecks": 6,
    "violationsFound": 0,
    "latencyMs": 2340
  }
}
```

### Verdicts

| Verdict | What it means | What happens |
|---------|---------------|--------------|
| **PASS** | Both LLMs agree, no compliance issues | Response delivered to customer |
| **WARN** | Minor discrepancy or soft issue | Response delivered with review flag |
| **BLOCK** | Hallucination, hard compliance violation, or major disagreement | Safe fallback message delivered instead |

### Other Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /api/v1/audit/stats` | Pass/warn/block counts |
| `GET /api/v1/audit` | List all audit records (filterable by verdict) |
| `GET /api/v1/audit/{id}` | Full audit trail for a single query |
| `GET /actuator/health` | Service health check |

## Dashboard UI

A React dashboard is included at `sentinel-ui/` for demo purposes. It runs with **mock data** out of the box — no backend or API keys needed.

```bash
cd sentinel-ui
npm install && npm run dev
# Opens at http://localhost:3000
```

The dashboard shows:
- **Overview** — stats, charts, pipeline explanation, compliance rules
- **Query Tester** — submit queries and see PASS/WARN/BLOCK results with grounding indicators
- **Audit Log** — browse all audit records with filtering
- **Audit Detail** — full breakdown of both LLM responses, audit reasoning, and delivered response

When the backend API is running, the UI automatically switches from mock data to live API calls.

## API Testing

An HTTP test file is included at `sentinel-ai.http` with 10 pre-built scenarios covering PASS, WARN, and BLOCK cases. Open it in IntelliJ or VS Code (REST Client extension) and click to run each request.

## Running

### API

```bash
cp .env.example .env
# Add your OPENAI_API_KEY and ANTHROPIC_API_KEY to .env

./gradlew bootRun
# API available at http://localhost:8080
```

### UI

```bash
cd sentinel-ui
npm install
npm run dev
# Dashboard available at http://localhost:3000
```

The UI works standalone with mock data — no backend required for demos.

## Compliance Rules

The system checks every response against SEC/FINRA regulations:

- No guaranteed returns or risk-free claims
- Suitability disclaimers required when giving advice
- Fair and balanced disclosure (risks alongside benefits)
- No unauthorized tax/legal advice
- Anti-discrimination (fair lending)
- Factual accuracy verified against provided context

Rules are configurable via JSON — no code changes needed to add or modify rules.
