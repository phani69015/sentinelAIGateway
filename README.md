# Sentinel AI

Enterprise-grade self-auditing LLM gateway for financial institutions.

## Problem

Financial institutions cannot deploy LLMs for customer-facing advice due to the high risk of hallucinated data causing regulatory breaches (SEC/FINRA violations).

## Solution

Sentinel AI is an API gateway that sits between a financial institution's customer-facing application and LLM providers. Every customer query passes through a triple-agent validation pipeline before any response is delivered.

```
Customer Query
      │
      ├──► Responder A (OpenAI GPT-4o)  ──┐
      │                                    ├──► Audit Agent ──► Validated Response
      └──► Responder B (Anthropic Claude) ─┘        │
                                                    ▼
                                           ┌────────────────┐
                                           │ Consistency     │
                                           │ Hallucination   │
                                           │ Compliance      │
                                           │ Toxicity        │
                                           └────────────────┘
                                                    │
                                              PASS / WARN / BLOCK
                                                    │
                                              PostgreSQL
                                           (immutable audit trail)
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 (LTS, virtual threads) |
| Framework | Spring Boot 4.0.6 |
| Build | Gradle 9.2.1 (Groovy DSL) |
| LLM Providers | OpenAI + Anthropic |
| Database | PostgreSQL 16 |
| Concurrency | Virtual Threads + StructuredTaskScope |
| Containers | Podman Compose |
| Testing | JUnit 5 + Testcontainers |

## Quick Start

### Prerequisites

- Java 25+
- Podman (with `podman compose`)
- OpenAI API key
- Anthropic API key

### Setup

```bash
# Clone and enter project
cd sentinel-ai

# Copy environment config
cp .env.example .env

# Add your API keys to .env
# OPENAI_API_KEY=sk-...
# ANTHROPIC_API_KEY=sk-ant-...

# Start infrastructure (PostgreSQL + App)
podman compose up -d

# Or run locally without containers:
./gradlew bootRun
```

### Usage

**Submit a query:**

```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the interest rate on your high-yield savings account?",
    "context": {
      "customerSegment": "retail",
      "productCategory": "savings",
      "knowledgeBase": [
        "Premium Savings: 4.25% APY on balances over $10,000",
        "FDIC insured up to $250,000",
        "6 free withdrawals per month"
      ]
    }
  }'
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

**Retrieve audit record:**

```bash
curl http://localhost:8080/api/v1/audit/{auditId}
```

**Get statistics:**

```bash
curl http://localhost:8080/api/v1/audit/stats
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/query` | Process a customer query through the Sentinel pipeline |
| GET | `/api/v1/audit/{id}` | Retrieve a specific audit record |
| GET | `/api/v1/audit` | List audit records (filterable by verdict) |
| GET | `/api/v1/audit/stats` | Aggregate pass/warn/block statistics |
| GET | `/actuator/health` | Health check |

## Architecture

### Pipeline Flow

1. **Query received** via REST API
2. **Parallel execution** — OpenAI and Anthropic answer independently (virtual threads)
3. **Audit Agent evaluation:**
   - Rule-based consistency check (numerical claims, contradictions)
   - Hallucination detection (ungrounded claims, cross-reference)
   - SEC/FINRA compliance scan (configurable JSON rule engine)
   - Toxicity filter (discrimination, aggressive sales, profanity)
   - LLM-powered deep analysis (nuanced reasoning via a third LLM call)
4. **Verdict determination** — PASS, WARN, or BLOCK
5. **Audit trail persisted** — immutable PostgreSQL record
6. **Response delivered** — validated answer or safe fallback

### Verdicts

| Verdict | Meaning | Action |
|---------|---------|--------|
| **PASS** | Both agree, compliant, no issues | Deliver response |
| **WARN** | Minor discrepancy or soft compliance issue | Deliver with review flag |
| **BLOCK** | Hallucination, hard violation, or major disagreement | Return safe fallback, alert compliance |

### SEC/FINRA Compliance Rules

The rule engine (`src/main/resources/rules/sec-finra-rules.json`) checks for:

- No guaranteed returns
- Suitability disclaimers required for advice
- Fair and balanced disclosure (risks alongside benefits)
- No unauthorized tax/legal advice
- Anti-discrimination
- Factual accuracy (cross-validated between providers)

Rules are JSON-configurable — add or modify without code changes.

## Project Structure

```
src/main/java/com/sentinel/ai/
├── config/              # App, LLM provider, ObjectMapper configuration
├── controller/          # REST endpoints (Query, Audit)
├── exceptions/          # Global exception handling
├── model/
│   ├── entity/          # JPA entities (AuditRecord, ComplianceViolation)
│   ├── dto/             # Request/Response DTOs
│   └── enums/           # AuditVerdict, ProviderType, ViolationType
├── repository/          # Spring Data JPA repositories
├── service/
│   ├── orchestrator/    # Pipeline coordinator + parallel executor
│   ├── llm/             # Provider interface + OpenAI/Anthropic implementations
│   ├── audit/           # AuditAgent, ConsistencyChecker, HallucinationDetector, etc.
│   └── compliance/      # Rule engine (future expansion)
└── util/                # Helpers
```

## Development

```bash
# Compile
./gradlew compileJava

# Run unit tests
./gradlew test

# Run integration tests (requires Docker/Podman for Testcontainers)
./gradlew integrationTest

# Full build with code quality checks
./gradlew build

# Format code (Palantir Java Format)
./gradlew spotlessApply
```

## Configuration

All configuration is via environment variables or `application.yaml`:

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENAI_API_KEY` | OpenAI API key | — |
| `ANTHROPIC_API_KEY` | Anthropic API key | — |
| `AUDIT_PROVIDER` | Which provider runs the audit agent | `ANTHROPIC` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `sentinel` |
| `DB_USER` | Database user | `sentinel` |
| `DB_PASSWORD` | Database password | `sentinel_secure_password` |
| `SERVER_PORT` | Application port | `8080` |
| `LOG_LEVEL` | Root log level | `INFO` |

## Design Decisions

| Decision | Rationale |
|----------|-----------|
| Virtual threads over reactive | Simpler blocking-style code that scales to thousands of concurrent LLM calls |
| Audit Agent uses an LLM | Enables nuanced reasoning about consistency rather than just pattern matching |
| Immutable audit records | Compliance requirement — no UPDATE/DELETE on audit table |
| JSON-configurable rules | Institutions can customize compliance rules without code changes |
| Dual-provider architecture | Provider diversity reduces correlated hallucination risk |
| Rule-based + LLM-based audit | Defense in depth — rules catch known patterns, LLM catches novel issues |

## License

Proprietary. Internal use only.
