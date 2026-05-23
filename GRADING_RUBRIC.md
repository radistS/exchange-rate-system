# Marcura Assessment — Grading Rubric Self-Review

> **Assessment:** Exchange Rate Management System (§10 Grading Rubric)  
> **Review date:** 2026-05-23  
> **Repository:** https://github.com/radistS/exchange-rate-system  
> **Reviewer:** Self-assessment (pre-submission)

This document maps the submission against the official rubric weights from the Marcura brief and `PLAN.md`. Scores are **estimated** — the review panel makes the final judgment.

---

## Rubric weights (100 points)

| # | Area | Weight | Deliverable (brief) |
|---|------|--------|---------------------|
| 1 | Backend API + persistence + scheduler | **25%** | Fixer ingest, spread formula, `/exchange`, `/analytics`, concurrency-safe counters |
| 2 | Angular (3 views) | **20%** | Calculator · Historical (table + chart + insight) · Analytics dashboard |
| 3 | AI trend insight | **20%** | Spring AI → Ollama, real rate data in prompt, `/exchange/insight` |
| 4 | AI-augmented workflow | **25%** | `PLAN.md`, agent rules, README AI Workflow, `[AI]` commits |
| 5 | Engineering quality | **10%** | OpenAPI, tests, README, `docker compose up` |

---

## 1. Backend — 24.0 / 25 (96%)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Fixer.io ingestion | ✅ Complete | `FixerClient`, `RateIngestionService`, daily `RateIngestionScheduler` (12:05 GMT, ShedLock) |
| Startup / backfill | ✅ Complete | `RateIngestionStartupRunner`, `POST /admin/refresh?from=&to=` |
| Spread formula (Appendix B) | ✅ Complete | `RateCalculationService.compute()`, `CurrencySpreadProvider` |
| `GET /exchange` (Appendix A) | ✅ Complete | `ExchangeResponse`: from, to, exchange, date, fromQueryCount, toQueryCount |
| `GET /analytics` (Appendix A) | ✅ Complete | `AnalyticsResponse.topCurrencies[]` |
| Concurrent usage counters | ✅ Complete | `CurrencyUsageRepository.upsertIncrement()` — SQL `ON CONFLICT DO UPDATE SET query_count = query_count + 1` |
| Counter concurrency test | ✅ Complete | `CurrencyUsageRepositoryConcurrentIT` |
| PostgreSQL + migrations | ✅ Complete | Flyway V1–V3, JPA validate |
| Demo without Fixer key | ✅ Bonus | `sample-rates.json`, `SampleRateSeedService`, 30-day seed |
| Integration test | ✅ Complete | `ExchangeControllerIT` (Testcontainers + MockMvc) |

**Gaps (−1.0 pt):** Live Fixer historical API requires a paid plan; mitigated by sample seed and documented in README.

**Key tests:** 19 backend tests, `BUILD SUCCESS` (verified 2026-05-23).

---

## 2. Angular — 19.0 / 20 (95%)

| View | Required | Status | Evidence |
|------|----------|--------|----------|
| Calculator | ✅ | ✅ | Currency pair, MatDatepicker, `GET /exchange`, `ViewState` |
| Historical | ✅ | ✅ | Table + ng2-charts line chart + AI insight panel |
| Analytics | ✅ | ✅ | Bar chart + usage table, auto-load on init |
| Home (extra) | — | ✅ Bonus | Currency list, how-to guide, spread groups |

**Gaps (−1.0 pt):** Frontend unit tests are scaffold-level only (ApiService + Calculator); not required heavily by rubric but below a polished submission.

**Build:** `npm run build` succeeds; Docker frontend image builds.

---

## 3. AI trend insight — 17.5 / 20 (88%)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Spring AI + Ollama | ✅ Complete | `AiConfig`, `spring-ai-starter-model-ollama` |
| Real rate JSON in prompt | ✅ Complete | `TrendInsightService` — `ratesJson` from `historicalAdjusted()` |
| `GET /exchange/insight` (Appendix A) | ✅ Complete | from, to, fromDate, toDate, insight |
| Deterministic fallback | ✅ Complete | Percent change summary when Ollama unavailable |
| Shared history logic | ✅ Complete | `ExchangeRateService.historicalAdjusted()` — no duplication |

**Gaps (−2.5 pt):**
- Reviewer must manually verify insight text references the selected period (requires `ollama pull llama3.2`).
- No dedicated integration test for `/exchange/insight` endpoint.

---

## 4. AI-augmented workflow — 22.5 / 25 (90%)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| Up-front plan | ✅ Complete | `PLAN.md` — rubric-ordered phases |
| Agent rules | ✅ Complete | `.cursor/1-project.md`, `2-be-java.md`, `3-fe-angular.md` |
| Agent context file | ✅ Complete | `CLAUDE.md` |
| README AI Workflow (§8.2) | ✅ Complete | Overrides, tool table, commit convention |
| `[AI]` git history | ✅ Complete | 8 commits on `main`, logical split |
| GitHub repository | ✅ Complete | https://github.com/radistS/exchange-rate-system |
| Screen recording | ❌ Pending | Listed in README §9 checklist — **candidate action** |

**Documented overrides (reviewer signal):**
1. SQL counters vs `AtomicLong`
2. `historicalAdjusted()` extraction
3. Sample rates in JSON vs hardcoded Java
4. Calculator `nonNullable` form fix

**Gaps (−2.5 pt):** Screen recording (3–5 min app + Cursor session) not yet attached to submission.

---

## 5. Engineering quality — 9.0 / 10 (90%)

| Criterion | Status | Evidence |
|-----------|--------|----------|
| OpenAPI / Swagger | ✅ Complete | springdoc, `*Api` interfaces, `/swagger-ui.html` |
| Unit + integration tests | ✅ Complete | 6 test classes, 19 tests passing |
| Docker Compose | ✅ Complete | postgres, ollama, backend, frontend + healthchecks |
| README | ✅ Complete | Architecture, setup, AI workflow, assumptions, checklist |
| RFC 7807 errors | ✅ Complete | `GlobalExceptionHandler` → `ProblemDetail` |
| BigDecimal discipline | ✅ Complete | Enforced in rules + tests |

**Gaps (−1.0 pt):** No CI pipeline (not required by brief).

---

## Weighted total

| Area | Weight | Section % | **Weighted score** |
|------|--------|-----------|-------------------|
| Backend | 25% | 96% | **24.0** |
| Angular | 20% | 95% | **19.0** |
| AI trend insight | 20% | 88% | **17.5** |
| AI-augmented workflow | 25% | 90% | **22.5** |
| Engineering | 10% | 90% | **9.0** |
| **Total** | **100%** | | **92.0 / 100** |

### Grade band

| Score | Interpretation |
|-------|----------------|
| 90–100 | Strong submission — ready for panel review |
| 80–89 | Solid — minor gaps in workflow or demo |
| 70–79 | Core works — workflow/docs weak |
| &lt; 70 | Missing rubric deliverables |

**Current estimate: 92 / 100** — strong technical delivery; complete screen recording to reach ~94–95.

---

## Appendix A / B compliance

| Appendix | Requirement | Status |
|----------|-------------|--------|
| A | `GET /exchange` response shape | ✅ |
| A | `GET /analytics` response shape | ✅ |
| A | `GET /exchange/insight` response shape | ✅ |
| B | Spread groups (0 / 3.25 / 4.50 / 6.00 / 2.75%) | ✅ `CurrencySpreadProvider` |
| §6.2 | Worked example EUR/PLN → 4.44 (4% spread) | ✅ `RateCalculationServiceTest` |
| Appendix B | EUR/PLN → 4.4978125000 | ✅ `ExchangeControllerIT` |

---

## Pre-submission checklist

| Item | Status |
|------|--------|
| `docker compose up --build` | ☐ Verify locally |
| `cd backend && mvn test` (19 tests) | ✅ Verified 2026-05-23 |
| `docker compose exec ollama ollama pull llama3.2` | ☐ One-time |
| Swagger lists all endpoints | ☐ Verify locally |
| Historical insight non-generic | ☐ Verify with Ollama |
| Screen recording (3–5 min) | ☐ **Required** |
| GitHub repo public/accessible | ✅ |

---

## How to re-run verification

```bash
cd backend && mvn test
cd frontend && npm run build
docker compose up --build
docker compose exec ollama ollama pull llama3.2
curl "http://localhost:8080/exchange?from=EUR&to=PLN"
curl "http://localhost:8080/analytics"
curl "http://localhost:8080/exchange/insight?from=EUR&to=GBP&fromDate=2026-04-23&toDate=2026-05-22"
```

---

*This file is a self-assessment aid for the candidate. The Marcura review panel applies the official rubric independently.*
