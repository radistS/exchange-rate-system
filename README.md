# Marcura — Exchange Rate Management System

Full-stack solution to the Marcura senior full-stack assessment. Spring Boot backend + Angular SPA + Spring AI trend insight, packaged so a reviewer can run the whole stack locally with Docker Compose or three terminal commands.

> **Quick links:** [PLAN.md](./PLAN.md) (up-front planning artefact) · [.cursor/](./.cursor/) (Cursor agent rules) · [GitHub](https://github.com/radistS/exchange-rate-system) · [Swagger UI](http://localhost:8080/swagger-ui.html) (after backend boot)

---

## 1. Architecture

```
┌─────────────────────────┐   HTTP   ┌──────────────────────────┐
│  Angular 19 SPA         │  ──────▶ │  Spring Boot 3.4 (Java 21)│
│  · Home (guide)         │          │  · REST controllers       │
│  · Calculator           │          │  · Services + JPA         │
│  · Historical + Chart   │          │  · Scheduler (ShedLock)   │
│  · Analytics dashboard  │          │  · Spring AI ChatClient   │
└─────────────────────────┘          └─────────────┬─────────────┘
                                                   │
                  ┌────────────────────────────────┼────────────────────────────┐
                  ▼                                ▼                            ▼
          ┌─────────────────┐             ┌────────────────┐           ┌──────────────────┐
          │ PostgreSQL 16   │             │ Fixer.io API   │           │ Ollama           │
          │  rates + usage  │             │  (daily fetch) │           │  (local LLM)     │
          │  + shedlock     │             └────────────────┘           └──────────────────┘
          └─────────────────┘
```

Single Spring Boot service. PostgreSQL stores USD-normalised rates and usage counters; ShedLock prevents duplicate daily ingestion across instances. Layering is `controller → service → repository`; entities and DTO records are mapped explicitly in services.

**Package root:** `com.marcura.exchangerate`

---

## 2. Local setup

### Prerequisites

- Docker Desktop (or Docker Engine + Compose v2) — recommended
- JDK 21 + Maven (backend dev/tests)
- Node 20 + npm (frontend dev)
- A [Fixer.io](https://fixer.io/) API key — **optional**; without it the app seeds 30 days of demo rates from `sample-rates.json`

### One-time

```bash
cp .env.example .env
# Edit .env to set FIXER_API_KEY, or leave empty to use sample data.
```

### Option A — Full stack (recommended for reviewers)

```bash
docker compose up --build
```

Wait until all services are healthy (~1 minute on first boot for Flyway + Spring).

Pull the Ollama model once (required for AI insights):

```bash
docker compose exec ollama ollama pull llama3.2
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Ollama | http://localhost:11434 |

**Rates load automatically on startup:**

- With `FIXER_API_KEY` — backfills missing Fixer rates for the last 30 days; daily job at **12:05 AM GMT**.
- Without Fixer key — `SampleRateSeedService` inserts demo rates for 18 currencies (EUR base) for the last 30 days.

Manual Fixer refresh (requires API key):

```bash
curl -X POST http://localhost:8080/admin/refresh
curl -X POST "http://localhost:8080/admin/refresh?from=2026-04-23&to=2026-05-22"
```

### Option B — Split dev (backend + frontend on host)

```bash
# 1. Database (+ optional Ollama)
docker compose up -d postgres ollama
docker compose exec ollama ollama pull llama3.2

# 2. Backend
cd backend && mvn spring-boot:run
# → http://localhost:8080

# 3. Frontend (new terminal)
cd frontend && npm install && npm start
# → http://localhost:4200
```

Backend reads env vars from the shell or `.env` (via Compose). Frontend reads `src/environments/environment.ts` locally, or `src/assets/runtime-config.json` in Docker (`API_URL` in `.env`).

### Run the tests

```bash
cd backend && mvn test          # 19 tests — unit + Testcontainers integration
cd frontend && npm test         # Jasmine specs for ApiService + Calculator
```

Backend integration tests use Testcontainers and pull `postgres:16-alpine` automatically.

---

## 3. AI Workflow

This section is required by the brief (§8.2) and reviewed under the **AI-Augmented Workflow** rubric (25 % of the grade).

### Tool & configuration

Primary agent: **Cursor** (Agent mode) with rules loaded from `.cursor/` and an upfront plan in `PLAN.md`.

| File | Purpose |
|------|---------|
| [`PLAN.md`](./PLAN.md) | Phase-by-phase plan produced with the agent *before* bulk implementation. Ordered by rubric weight so time tracks marks. |
| [`.cursor/1-project.md`](./.cursor/1-project.md) | Repo-wide rules — BigDecimal, Fixer `rate_date` source, concurrency strategy, layering, commit conventions. |
| [`.cursor/2-be-java.md`](./.cursor/2-be-java.md) | Backend-scoped — constructor injection, record DTOs, `@Transactional`, Spring AI prompt rules. Glob: `backend/**/*.java`. |
| [`.cursor/3-fe-angular.md`](./.cursor/3-fe-angular.md) | Frontend-scoped — standalone components, `ViewState`, single `ApiService`, strict TypeScript. Glob: `frontend/**/*.{ts,html,scss}`. |

Scoping backend and frontend rules to their globs prevents Java conventions leaking into TypeScript (and vice versa) during cross-layer agent sessions.

### How the agent was used across the development cycle

**1. Planning — before code.**

The assessment brief and rubric weights were pasted into Cursor. `PLAN.md` was generated first — mapping backend (25 %), Angular (20 %), AI insight (20 %), workflow (25 %), and engineering (10 %) to concrete files and phases.

**2. Multi-file agentic sessions — full layers at once.**

Each phase ran as a single Cursor session producing every file for that layer: entities + Flyway migrations + repositories; then services + OpenAPI interfaces + controllers; then Angular features + `ApiService`; then Docker Compose and tests.

**3. Test generation — agent-generated, hand-reviewed.**

`RateCalculationServiceTest` includes the brief's §6.2 worked example (EUR/PLN → **4.44** at 4 % max spread) and Appendix B coverage. `ExchangeControllerIT` (Testcontainers + MockMvc) verifies spread-adjusted rates and counter increments. `CurrencyUsageRepositoryConcurrentIT` validates atomic counter behaviour under concurrent threads.

**4. Refinement sessions — ongoing critical review.**

Post-implementation passes fixed: duplicate spread-adjusted series logic (extracted to `ExchangeRateService.historicalAdjusted()`), Fixer historical backfill + startup seed, Angular strict-null build errors on calculator forms, and test expectation alignment for Appendix B precision (**4.4978125000**).

**5. Documentation — drafted by agent, corrected by hand.**

This README and `PLAN.md` were drafted in agent sessions and hand-edited for accuracy (actual endpoint paths, Flyway not Liquibase, Cursor not Claude Code, Angular 19).

### Overrides

**Override 1 — concurrency strategy (`CurrencyUsageRepository`).**

An early agent draft used in-memory `AtomicLong` for query counts. The brief requires correctness under concurrent requests and multiple instances. Override: native SQL `INSERT … ON CONFLICT DO UPDATE SET query_count = query_count + 1` — atomicity from the Postgres row lock. Rule encoded in `.cursor/1-project.md`; verified in `CurrencyUsageRepositoryConcurrentIT`.

**Override 2 — duplicated historical computation.**

`HistoricalController` and insight generation initially duplicated the spread-adjusted series logic. Override: shared `ExchangeRateService.historicalAdjusted()` used by history and `TrendInsightService`.

**Override 3 — sample rates in JSON, not hardcoded Java.**

Demo rates live in `backend/src/main/resources/data/sample-rates.json` (and a frontend copy for the Home page currency list). `SampleRateSeedService` normalises Fixer-style EUR-base quotes to USD-per-unit on ingest — editable without recompile.

**Override 4 — frontend TypeScript strictness.**

Calculator form used `fb.group()`; strict mode flagged `from`/`to` as possibly null. Override: `fb.nonNullable.group()` for currency fields + nullable `date` control with Material datepicker.

### Commit-prefix convention

- `[AI] feat: …` / `[AI] test: …` / `[AI] chore: …` / `[AI] docs: …` — bulk of the diff produced by an agent session, reviewed and merged.
- Plain Conventional Commits — hand-edits, overrides, and corrections.

Current history (6 commits on `main`): plan → rules → backend → frontend → Docker → README.

---

## 4. AI provider setup (Spring AI)

The app uses **Ollama** via `spring-ai-starter-model-ollama`.

### Ollama setup (Docker Compose)

```bash
docker compose up -d ollama
docker compose exec ollama ollama pull llama3.2
```

Configure in `.env`:

```env
OLLAMA_BASE_URL=http://ollama:11434    # http://localhost:11434 outside Compose
OLLAMA_MODEL=llama3.2
```

Any chat model Ollama supports works. The system prompt asks for 2–4 sentences grounded in the provided JSON rate series.

### Fallback

If Ollama is unreachable, `TrendInsightService` logs a warning and returns a deterministic summary computed from first/last rate in the period. The UI displays it normally — useful when the model is not pulled yet.

---

## 5. Endpoints (Swagger UI is the canonical reference)

| Method | Path | Description |
|--------|------|-------------|
| GET | `/exchange` | Spread-adjusted rate for a pair; optional `date`. Increments usage counters. |
| GET | `/exchange/history` | Daily spread-adjusted rate series (`from`, `to`, `fromDate`, `toDate`). |
| GET | `/exchange/insight` | LLM trend commentary for a pair + date range. |
| GET | `/analytics` | Top currencies by query count (`topCurrencies[]`). |
| POST | `/admin/refresh` | Manual Fixer ingest; optional `from`/`to` for range backfill. |
| GET | `/swagger-ui.html` | Swagger UI. |
| GET | `/api-docs` | OpenAPI 3 JSON. |

Response shapes match **Appendix A** (`exchange`, `analytics`, `insight`). Errors are RFC 7807 `ProblemDetail` JSON; the frontend `ApiService` normalises failures into a consistent error shape.

---

## 6. Spread calculation & concurrency

**Formula** (single implementation in `RateCalculationService.compute()`):

```
adjustedRate = (toRateToUsd / fromRateToUsd) × ((100 − max(fromSpread, toSpread)) / 100)
```

- Spreads: Appendix B in `CurrencySpreadProvider` (base currency 0 %; JPY/HKD/KRW 3.25 %; MYR/INR/MXN 4.50 %; RUB/CNY/ZAR 6.00 %; default 2.75 %).
- §6.2 worked example → **4.44** (pedagogical 4 % spread).
- Appendix B EUR/PLN (stored rates 0.8 / 3.7, PLN 2.75 %) → **4.4978125000** at scale 10.

**Usage counters:** each `GET /exchange` increments both currencies via `CurrencyUsageRepository.upsertIncrement()` — one atomic SQL statement per currency, safe across threads and app instances.

---

## 7. Assumptions & trade-offs

| Topic | Decision |
|-------|----------|
| Fixer base currency | Rates stored USD-normalised after ingest; formula is symmetric; EUR base gets 0 % spread per Appendix B. |
| Scheduler | ShedLock JDBC — honest multi-instance behaviour without K8s leader election. |
| Counter atomicity | Postgres `ON CONFLICT DO UPDATE` — no in-process locks. |
| BigDecimal | Storage `numeric(19, 8)`; computation `DECIMAL64`; JSON output scale 10. |
| Seed fallback | `sample-rates.json` + 30-day seed when no Fixer key; `insertIfAbsent` on startup avoids overwriting existing rows. |
| No auth | Out of scope; `/admin/refresh` would be role-gated in production. |
| No caching | YAGNI — Postgres lookups are fast enough for this assessment scope. |
| Frontend scope | Three rubric views (calculator, historical, analytics) plus a Home guide page; Material + ng2-charts; clarity over polish. |

---

## 8. Project layout

```
exchange-rate-system/
├── backend/                         # Spring Boot 3.4, Java 21, Maven
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/marcura/exchangerate/
│       │   ├── ExchangeRateApplication.java
│       │   ├── config/              # OpenAPI, CORS, ShedLock, AI, Fixer props
│       │   ├── client/              # FixerClient + FixerResponse
│       │   ├── domain/              # ExchangeRate, CurrencyUsage
│       │   ├── repository/          # JPA + JDBC upsert
│       │   ├── service/             # Spread, calculation, exchange, insight, ingestion, seed
│       │   ├── job/                 # Daily scheduler + startup runners
│       │   └── web/                 # Controllers, *Api interfaces, DTOs, exception handler
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── data/sample-rates.json
│       │   └── db/migration/        # Flyway V1–V3
│       └── test/java/               # Unit + Testcontainers IT
├── frontend/                        # Angular 19 SPA
│   └── src/app/
│       ├── core/                    # ApiService, models, currencies
│       ├── shared/                  # ViewState, date helpers
│       └── features/
│           ├── home/
│           ├── calculator/
│           ├── historical/
│           └── analytics/
├── docker-compose.yml               # postgres, ollama, backend, frontend
├── .env.example
├── PLAN.md
├── .cursor/                         # Cursor agent rules
└── README.md
```

---

## 9. Submission checklist

- [ ] `docker compose up --build` — all services healthy
- [ ] `cd backend && mvn test` — 19 tests green
- [ ] `docker compose exec ollama ollama pull llama3.2`
- [ ] Swagger UI lists all endpoints
- [ ] Historical page — chart + period-specific AI insight
- [ ] **Screen recording (3–5 min):** Home → Calculator → Historical → Analytics + short Cursor session
- [ ] GitHub repo with `[AI]` commit history — https://github.com/radistS/exchange-rate-system

---

## 10. License

Internal — Marcura R&D Technical Assessment.
