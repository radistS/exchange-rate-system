# Marcura Exchange Rate System — Implementation Plan

> **Stack (fixed):** Java 21 · Spring Boot 3.x · Maven · PostgreSQL · Angular 17+ · Spring AI · Ollama · Docker Compose  
> **Principles:** KISS · DRY · YAGNI — ship what the rubric grades; defer optional extras until core paths work.

## 1. Assessment mapping (what to build)

| Area | Weight | Deliverable |
|------|--------|-------------|
| Backend API + persistence + scheduler | 25% | Fixer ingest, spread formula, `/exchange`, `/analytics`, concurrency-safe counters |
| Angular (3 views) | 20% | Calculator · Historical table + chart + insight · Analytics dashboard |
| AI trend insight | 20% | Spring AI → Ollama, prompt with real rate data, `/exchange/insight` |
| AI-augmented workflow | 25% | This file, `.cursor/rules`, README “AI Workflow”, `[AI]` commits |
| Engineering (OpenAPI, tests, README) | 10% | springdoc, unit + 1 integration test, runnable `docker compose` |

**YAGNI (explicitly out of scope for v1):** auth, RAG/vector DB, manual refresh endpoint, K8s, metrics stack, multi-tenant, fine-tuned models, production HA beyond ShedLock + idempotent upsert.

---

## 2. Architecture (KISS)

```
┌─────────────┐     HTTP      ┌──────────────────────────────────────┐
│   Angular   │ ────────────► │  Spring Boot (single deployable)      │
│  ng serve   │               │  • REST controllers                   │
└─────────────┘               │  • ExchangeRateService (formula)      │
                              │  • FixerClient + RateIngestionJob     │
                              │  • UsageCounterService                │
                              │  • TrendInsightService (Spring AI)    │
                              └───────┬──────────────┬────────────────┘
                                      │              │
                              ┌───────▼──────┐  ┌──────▼──────┐
                              │ PostgreSQL   │  │   Ollama    │
                              │ rates, usage │  │  (llama3.2) │
                              └──────────────┘  └─────────────┘
                                      ▲
                              ┌───────┴──────┐
                              │  Fixer.io    │  (daily @ 12:05 GMT)
                              └──────────────┘
```

**Monorepo layout**

```
exchange-rate-system/
├── backend/                 # Spring Boot Maven module
├── frontend/                # Angular CLI app
├── docker-compose.yml       # db, ollama, backend, frontend
├── PLAN.md                  # (this file)
├── .cursor/rules/
└── README.md
```

**Layering (backend)** — thin controllers, fat enough services, no extra hexagon:

- `web` — REST + DTOs + exception handler (`@ControllerAdvice`)
- `service` — business rules (spread, insight orchestration, usage increment)
- `client` — Fixer HTTP (WebClient or RestClient)
- `repository` — Spring Data JPA
- `domain` — entities + enums (spread group)
- `config` — scheduler, ShedLock, Spring AI, CORS, OpenAPI

---

## 3. Data model

### 3.1 `exchange_rate`

Stores Fixer rates **per currency per API date** (not fetch timestamp).

| Column | Type | Notes |
|--------|------|--------|
| `id` | BIGSERIAL PK | |
| `currency_code` | CHAR(3) | ISO code |
| `rate_to_usd` | NUMERIC(20,10) | From Fixer (`rates` map vs USD base) |
| `rate_date` | DATE | **From API** (`date` field in response) |
| `created_at` | TIMESTAMPTZ | Audit only |

**Unique constraint:** `(currency_code, rate_date)` — upsert on conflict (ignore or update same values).

### 3.2 `currency_usage`

Concurrent-safe query counters for analytics.

| Column | Type | Notes |
|--------|------|--------|
| `currency_code` | CHAR(3) PK | |
| `query_count` | BIGINT NOT NULL DEFAULT 0 | |
| `last_queried_at` | DATE | Last day a query touched this currency |

**Increment strategy (graded):** single SQL `UPDATE ... SET query_count = query_count + 1, last_queried_at = GREATEST(...)` — atomic, no application-level read-modify-write. Document in README why this beats synchronized blocks.

Optional: `currency_usage_log` (currency, queried_date) **only if** analytics needs per-day breakdown beyond `last_queried_at` — start without it; add one row per query in same transaction as counter bump **only if** dashboard needs daily histogram (YAGNI: try aggregating from `last_queried_at` + count first; if insufficient, add slim log table).

### 3.3 Spread configuration

**No DB table** — static map in code (Appendix B), keyed by currency code:

| Group | Spread % |
|-------|----------|
| Base (Fixer base currency, typically EUR) | 0.00 |
| JPY, HKD, KRW | 3.25 |
| MYR, INR, MXN | 4.50 |
| RUB, CNY, ZAR | 6.00 |
| All others | 2.75 |

`SpreadService.getSpread(String code) → BigDecimal` — single source for formula and tests.

### 3.4 Migrations

Flyway `V1__init.sql` — keeps schema reproducible in Docker and CI.

---

## 4. Exchange rate calculation (must be exact)

**Formula** (use `BigDecimal`, scale 10+, `HALF_UP` for API response):

```
pairRate = toRateToUsd / fromRateToUsd
spread   = max(spread(from), spread(to))
adjusted = pairRate × (100 − spread) / 100
```

**Service flow (`ExchangeRateService`):**

1. Resolve `rate_date` = param date or `MAX(rate_date)` in DB.
2. Load both currencies’ `rate_to_usd` for that date — if either missing → `404` + clear message.
3. Compute adjusted rate.
4. In **same transaction**: increment usage for `from` and `to` (two atomic updates).
5. Return DTO matching Appendix A: `from`, `to`, `exchange`, `date`, `fromQueryCount`, `toQueryCount`.

**DRY:** one private method `computeAdjustedRate(fromRate, toRate, fromCode, toCode)` — unit-tested with assessment example (EUR/PLN → 4.44).

---

## 5. Backend API contract

Align with Appendix A; prefix optional (`/api` vs root — pick one, document in OpenAPI).

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/exchange` | Query params: `from`, `to`, optional `date` (ISO) |
| GET | `/analytics` | Usage stats for dashboard |
| GET | `/exchange/history` | Table data: `from`, `to`, `fromDate`, `toDate` → list of `{ date, rate }` (raw or adjusted — **pick adjusted** for consistency with calculator; document choice) |
| GET | `/exchange/insight` | `from`, `to`, `fromDate`, `toDate` → `{ ..., insight }` |
| POST | `/admin/refresh` | **Optional** — manual Fixer fetch; no usage counter changes |
| GET | `/actuator/health` | Docker healthchecks |

**Errors:** 400 validation, 404 missing rates, 502/503 Fixer/LLM down with body `{ "message": "..." }`.

**OpenAPI:** `springdoc-openapi-starter-webmvc-ui` — all endpoints annotated; Swagger UI linked from README.

---

## 6. Fixer.io ingestion

### 6.1 Client

- `FixerClient.fetchLatest()` → DTO with `date` + `rates` map.
- API key: `FIXER_API_KEY` env var (never commit).
- Base URL from Fixer docs; handle 429/5xx with log + fail job (no infinite retry in v1).

### 6.2 Scheduled job

- Cron: `0 5 0 * * *` zone `GMT` (12:05 AM GMT).
- For each rate entry: upsert `(currency, rate_date from API, rate_to_usd)`.
- **Multi-instance:** [ShedLock](https://github.com/lukas-krecan/ShedLock) with JDBC provider — one lock name `fixer-daily-ingest`, `lockAtMostFor` ~ 30m. Justify in README: avoids duplicate Fixer calls without requiring single pod.

### 6.3 Manual refresh (optional, last)

Same upsert logic extracted to `RateIngestionService.ingest()` — called by scheduler and optional admin endpoint.

---

## 7. Spring AI + Ollama

### 7.1 Runtime (Docker Compose)

```yaml
ollama:
  image: ollama/ollama
  volumes: [ollama_data:/root/.ollama]
  # init: pull llama3.2 (or mistral) once — document in README
```

Backend env:

```properties
spring.ai.ollama.base-url=http://ollama:11434
spring.ai.ollama.chat.options.model=llama3.2
```

Local dev: same vars pointing to `localhost:11434`.

### 7.2 `TrendInsightService`

1. Load historical **adjusted** rates for pair + date range (reuse history query or shared repository method).
2. Build compact context string (CSV or bullet list: `date, rate`) — **actual numbers**, not summaries.
3. System prompt (keep in `prompts/trend-insight.txt` or constant):

   - Role: concise FX trend commentator for internal dashboard.
   - Constraints: 2–4 sentences, refer to dates/direction/magnitude, no investment advice, no invented data.
   - If &lt; 2 data points: say insufficient data.

4. `ChatClient` call (Spring AI) → parse text → return in `/exchange/insight`.
5. Timeout ~ 30s; on failure return 503 with friendly message (frontend shows error).

**YAGNI:** no streaming, no chat memory, no RAG, no function calling.

---

## 8. Angular frontend

**Angular 17+**, standalone components, strict TS, `provideHttpClient()`.

### 8.1 Structure

```
src/app/
├── core/           # API base URL from environment
├── shared/         # loading spinner, error banner, currency select
├── features/
│   ├── calculator/
│   ├── history/    # table + chart + insight panel
│   └── analytics/
└── app.routes.ts
```

**Environment:** `environment.apiUrl` from `API_URL` at build time or `fileReplacements` — README: `ng serve` with `environment.ts` pointing to `http://localhost:8080`.

### 8.2 Views

| View | Route | Behaviour |
|------|-------|-----------|
| Calculator | `/` or `/calculator` | Reactive form: from, to, optional date; call GET `/exchange`; loading + error display |
| Historical | `/history` | Pair + date range; parallel: history table + chart (ngx-charts or Chart.js); below chart call `/exchange/insight` with loading skeleton |
| Analytics | `/analytics` | Cards or bar chart from `/analytics` — top currencies, counts, last queried |

**KISS chart:** one line series, labeled axes, no animations required.

### 8.3 Types

Interfaces mirror backend DTOs (`ExchangeResponse`, `AnalyticsResponse`, `InsightResponse`) — one `exchange-api.service.ts`.

### 8.4 CORS

Backend `WebMvcConfigurer` allows `http://localhost:4200` (and frontend container origin if proxied).

---

## 9. Docker Compose (single command demo)

| Service | Image / build | Ports |
|---------|---------------|-------|
| `db` | postgres:16-alpine | 5432 |
| `ollama` | ollama/ollama | 11434 |
| `backend` | `backend/Dockerfile` multi-stage | 8080 |
| `frontend` | nginx serving `ng build` **or** dev profile omitted for submission | 4200 → 80 |

**Startup order:** db healthy → ollama up → backend (Flyway migrate on start) → frontend.

**Seed:** first run needs Fixer key; for review without key, document optional `data.sql` fixture for one date (dev profile only).

**README must include:**

1. `cp .env.example .env` and set `FIXER_API_KEY`
2. `docker compose up --build`
3. Pull Ollama model: `docker compose exec ollama ollama pull llama3.2`
4. Wait for first scheduled job **or** hit optional refresh / SQL seed

---

## 10. Testing (minimum viable, AI-generated OK)

| Test | Type | What |
|------|------|------|
| `ExchangeRateServiceTest` | Unit | Appendix example EUR→PLN = 4.44; spread max logic; missing date |
| `SpreadServiceTest` | Unit | Each spread group boundary |
| `ExchangeRateControllerIT` | Integration | `@SpringBootTest` + Testcontainers PostgreSQL; seed rates; GET `/exchange` 200 + counter increment |
| Angular | Optional smoke | One service test with `HttpClientTestingModule` |

**Do not** chase 90% coverage — rubric wants spread logic + one integration path.

---

## 11. Implementation phases (time-boxed)

### Phase 0 — Prep (½ day)

- [ ] Finalize this plan; commit `[AI] docs: implementation plan`
- [ ] `.env.example`, update `.cursor/rules` with stack-specific notes
- [ ] Initialize `backend` (start.spring.io: Web, JPA, Validation, Flyway, Actuator, springdoc)
- [ ] Initialize `frontend` (`ng new`, routing, strict)

### Phase 1 — Backend core (1–1½ days) — **highest rubric weight**

- [ ] Flyway schema + entities + repositories
- [ ] `SpreadService` + `ExchangeRateService` + unit tests
- [ ] `GET /exchange` + exception handler + OpenAPI
- [ ] `CurrencyUsage` atomic increment in same `@Transactional` as exchange read

### Phase 2 — Ingestion (½ day)

- [ ] `FixerClient` + `RateIngestionService` upsert
- [ ] ShedLock + scheduled job 12:05 GMT
- [ ] Manual test with real API key; integration test with Testcontainers + seeded rates

### Phase 3 — History + analytics (½ day)

- [ ] `GET /exchange/history` (date range, computed rates)
- [ ] `GET /analytics`
- [ ] Repository queries: `findByCurrencyAndRateDateBetween`, top N usage

### Phase 4 — Spring AI (½ day)

- [ ] Spring AI Ollama starter + `TrendInsightService` + prompt file
- [ ] `GET /exchange/insight`
- [ ] Verify prompt receives real CSV in logs (debug flag off in prod)

### Phase 5 — Angular (1–1½ days)

- [ ] Core API service + environments
- [ ] Calculator view (complete polish: validation, errors, loading)
- [ ] History view (table + chart + insight panel)
- [ ] Analytics view
- [ ] Shell navigation + basic styling (Angular Material **optional** — CDK + simple CSS is fine)

### Phase 6 — Docker + docs (½ day)

- [ ] Dockerfiles + compose wiring + healthchecks
- [ ] README: setup, architecture diagram, Ollama steps, assumptions, trade-offs
- [ ] README: **AI Workflow** section (tool, config, disagreement example)
- [ ] Screen recording script (3–5 min): demo + brief agent session

### Phase 7 — Optional if time

- [ ] `POST /admin/refresh`
- [ ] Extra Angular tests
- [ ] `currency_usage_log` for richer analytics

---

## 12. Key decisions & trade-offs (for README)

| Decision | Choice | Why |
|----------|--------|-----|
| DB | PostgreSQL | Assessment allows any RDBMS; JSON/analytics not needed |
| Java 21 | Records for DTOs, virtual threads **not required** | Modern LTS without novelty |
| Scheduler locking | ShedLock JDBC | KISS vs custom advisory locks |
| Rate storage | USD-normalized from Fixer | Matches formula; base currency spread 0% |
| History endpoint | Adjusted rates | Same semantics as calculator |
| LLM | Ollama llama3.2 local | Assessment recommends; no API cost |
| No auth | Internal tool | YAGNI |

**Risks:** Fixer free tier limits / base currency; Ollama slow on laptop — document minimum RAM; first day empty DB until cron or manual ingest.

---

## 13. AI workflow deliverables (25% of grade)

- [x] `PLAN.md` (this file) — AI-assisted before bulk implementation
- [ ] `.cursor/rules` — stack, BigDecimal, no secrets, `[AI]` commit prefix
- [ ] README **AI Workflow**: Cursor, rules used, example override
- [ ] Commits prefixed `[AI]` for agent-generated chunks; human fixes separate or `[AI] fix: ...`

---

## 14. Submission checklist

- [ ] GitHub repo with meaningful history
- [ ] `docker compose up` works with documented env
- [ ] Swagger UI lists all endpoints
- [ ] 3–5 min screen recording (app + agent session)
- [ ] Spread calculation matches worked example
- [ ] Insight shows non-generic text tied to selected period
- [ ] Concurrent counter approach explained in README

---

## 15. References

- Assessment: `Marcura_Assessment.pdf`
- Fixer API: https://fixer.io/documentation
- Spring AI Ollama: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
- ShedLock: https://github.com/lukas-krecan/ShedLock
