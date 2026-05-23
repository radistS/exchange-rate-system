# Exchange Rate System

Marcura technical assessment — Fixer.io ingestion, spread-adjusted rates, Angular dashboard, Spring AI (Ollama) insights.

## Structure

```
exchange-rate-system/
├── backend/           # Java 21 · Spring Boot 3 · Maven
├── frontend/          # Angular 19 · Material · standalone components
├── docker-compose.yml # postgres, ollama, backend, frontend
├── PLAN.md            # AI-assisted implementation plan
├── .cursor/           # Cursor agent rules (repo, backend, frontend)
├── .env.example
└── README.md
```

## Prerequisites

- Docker Desktop (or Docker Engine + Compose v2)
- Java 21 (local backend/tests)
- Node 20+ (local frontend)
- [Fixer.io](https://fixer.io/) API key (optional — demo sample rates load without it)

## Docker Compose (recommended)

### 1. Configure environment

```bash
cp .env.example .env
```

Compose works without `.env` (built-in defaults). Edit `.env` to set `FIXER_API_KEY` for live Fixer data.

| Variable | Default | Purpose |
|----------|---------|---------|
| `FIXER_API_KEY` | *(empty)* | Fixer.io API access |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `exchange` | PostgreSQL credentials |
| `POSTGRES_DB` | `exchange_rates` | Database name |
| `OLLAMA_BASE_URL` | `http://ollama:11434` | Spring AI → Ollama |
| `OLLAMA_MODEL` | `llama3.2` | Chat model name |
| `API_URL` | `http://localhost:8080` | Backend URL in the browser |
| `FIXER_STARTUP_ENABLED` | `true` | Fixer backfill on startup |
| `FIXER_STARTUP_BACKFILL_DAYS` | `30` | Days to backfill from Fixer |
| `SAMPLE_RATES_ENABLED` | `true` | Demo seed when no Fixer key |
| `SAMPLE_RATES_DAYS` | `30` | Days of sample data to seed |

### 2. Start the stack

```bash
docker compose up --build
```

Wait until all services are healthy (first backend start ~1 minute for Flyway + Spring).

### 3. Pull an Ollama model (first time only)

```bash
docker compose exec ollama ollama pull llama3.2
```

Trend insights need this model (or change `OLLAMA_MODEL` in `.env`).

### 4. Exchange rates (automatic)

- **With `FIXER_API_KEY`:** startup backfills missing Fixer rates for the last 30 days; daily job at **12:05 AM GMT**.
- **Without Fixer key:** demo rates from `backend/src/main/resources/data/sample-rates.json` (18 currencies, EUR base).

Manual refresh (requires Fixer key):

```bash
# Latest day
curl -X POST http://localhost:8080/admin/refresh

# Date range backfill
curl -X POST "http://localhost:8080/admin/refresh?from=2026-04-23&to=2026-05-22"
```

### 5. Open the application

| Service | URL |
|---------|-----|
| Frontend | http://localhost:4200 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Ollama | http://localhost:11434 |

### Useful commands

```bash
docker compose down          # stop
docker compose down -v       # stop + wipe DB volume
docker compose logs -f backend
docker compose exec postgres psql -U exchange -d exchange_rates
```

## Local development (without full Compose)

| Layer | Command |
|-------|---------|
| Database | `docker compose up postgres` |
| Backend | `cd backend && mvn spring-boot:run` |
| Frontend | `cd frontend && npm install && npm start` |
| Tests | `cd backend && mvn test` |

## Spread calculation

Single formula in `RateCalculationService.compute()`:

```
adjustedRate = (toRateToUsd / fromRateToUsd) × ((100 − max(fromSpread, toSpread)) / 100)
```

- Spreads: Appendix B in `CurrencySpreadProvider` (EUR base = 0%, groups 3.25 / 4.50 / 6.00%, default 2.75%).
- Assessment §6.2 worked example (EUR 1%, PLN 4% → **4.44**) covered in unit tests.
- Appendix B EUR/PLN (0.8 / 3.7 USD rates, PLN 2.75%) → **4.4978125000** at scale 10.

## Concurrency-safe usage counters

Each `GET /exchange` increments counters for **both** currencies in the pair. Under concurrent requests and multiple app instances, counters must not lose updates.

**Approach:** one native SQL upsert per currency in `CurrencyUsageRepository.upsertIncrement()`:

```sql
INSERT INTO currency_usage (currency_code, query_count, last_queried_at)
VALUES (:currency, 1, :queriedDate)
ON CONFLICT (currency_code) DO UPDATE SET
    query_count = currency_usage.query_count + 1,
    last_queried_at = GREATEST(...)
```

PostgreSQL row-level locking on conflict makes `count + 1` atomic. **Not used:** `AtomicLong`, `synchronized`, or read-modify-write in Java — verified in `CurrencyUsageRepositoryConcurrentIT`.

## AI Workflow

This project was built with **Cursor** (Agent mode) using rules in `.cursor/` and an upfront plan in `PLAN.md`.

### Tooling and rules

| Artifact | Purpose |
|----------|---------|
| `PLAN.md` | Rubric-weighted implementation plan — written before bulk coding |
| `.cursor/1-project.md` | Repo-wide: BigDecimal, Fixer date, concurrency, layering |
| `.cursor/2-be-java.md` | Spring Boot: DTOs, `@Transactional`, Spring AI patterns |
| `.cursor/3-fe-angular.md` | Angular: standalone, `ViewState`, `ApiService` only |

### Typical workflow

1. **Plan first** — `PLAN.md` mapped assessment sections to files and phased delivery.
2. **Agent implements** — Cursor Agent generated backend, frontend, Docker, and tests in focused passes.
3. **Human review** — run `mvn test`, `docker compose up --build`, fix gaps.
4. **Commits** — `[AI] …` prefix when the agent produced the bulk of the diff; plain commits for manual overrides.

### Example: correcting the agent

**Concurrency counters:** An early agent draft used in-memory `AtomicLong` for query counts. The assessment requires correctness under concurrent HTTP requests and multiple instances. I overrode this with a **single SQL `INSERT … ON CONFLICT DO UPDATE SET query_count = query_count + 1`** in `CurrencyUsageRepository`, encoded the rule in `.cursor/1-project.md`, and added `CurrencyUsageRepositoryConcurrentIT`.

**Duplicate spread logic:** The first pass duplicated adjusted-rate series logic in `HistoricalController` and insight code. Extracted to `ExchangeRateService.historicalAdjusted()` — rule added to `.cursor/1-project.md` (“Duplication rule”).

**Frontend build:** Agent used `fb.group()` for calculator fields; strict TypeScript flagged `from`/`to` as possibly null. Fixed with `fb.nonNullable.group()` for currency fields and a nullable `date` control.

### What I would not delegate blindly

- Spread formula constants and test expectations (assessment §6.2 / Appendix B).
- Secrets (`.env` is gitignored; only `.env.example` is committed).
- Final rubric checklist and submission recording.

## Assumptions and trade-offs

| Decision | Rationale |
|----------|-----------|
| No authentication | Internal assessment tool (YAGNI) |
| USD-normalized storage | Matches spread formula; Fixer EUR base converted on ingest |
| Ollama local | No LLM API cost; deterministic fallback when model unavailable |
| Sample rate seed | App usable without Fixer paid plan / API key |
| ShedLock | Prevents duplicate daily ingestion in multi-instance deploys |
| `insertIfAbsent` on startup | Avoids overwriting existing DB rows on restart |

## Submission checklist

- [ ] `docker compose up --build` — all services healthy
- [ ] `cd backend && mvn test` — all tests green
- [ ] `docker compose exec ollama ollama pull llama3.2`
- [ ] Swagger UI lists all endpoints
- [ ] Historical page shows chart + non-generic AI insight for a 30-day range
- [ ] **Screen recording (3–5 min):** demo Home → Calculator → Historical → Analytics, plus a short Cursor agent session (plan or one fix)
- [ ] Meaningful git history with `[AI]` commits

## License

Assessment submission — internal use.
