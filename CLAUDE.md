# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Read **`PLAN.md`** before any non-trivial change — it is ordered by Marcura rubric weight, not by component. For Cursor-specific scoped rules see **`.cursor/`** (`1-project.md`, `2-be-java.md`, `3-fe-angular.md`).

---

## Project overview

Marcura senior full-stack assessment: **spread-adjusted exchange rates**, usage analytics, historical charts, and **Spring AI (Ollama) trend insights**.

| Layer | Stack |
|-------|--------|
| Backend | Java 21 · Spring Boot 3.4 · Maven · PostgreSQL · Flyway · ShedLock |
| Frontend | Angular 19 · Material · standalone components · ng2-charts |
| AI | Spring AI → Ollama (`llama3.2`) |
| Infra | Docker Compose (postgres, ollama, backend, frontend) |

**Package root:** `com.marcura.exchangerate`

**Principles:** KISS · DRY · YAGNI — ship what the rubric grades; defer auth, caching, K8s, dual LLM providers.

---

## Commands

```bash
# Full stack (reviewer path)
docker compose up --build
docker compose exec ollama ollama pull llama3.2

# Backend
cd backend && mvn spring-boot:run
cd backend && mvn test                    # 19 tests; Testcontainers pulls postgres:16-alpine

# Frontend
cd frontend && npm install && npm start
cd frontend && npm test
cd frontend && npm run build

# Manual Fixer ingest (needs FIXER_API_KEY in .env)
curl -X POST http://localhost:8080/admin/refresh
curl -X POST "http://localhost:8080/admin/refresh?from=2026-04-23&to=2026-05-22"
```

**URLs after boot:** Frontend http://localhost:4200 · API http://localhost:8080 · Swagger http://localhost:8080/swagger-ui.html

**Secrets:** Never commit `.env`. Only `.env.example` is tracked.

---

## Architecture

```
controller → service → repository → DB
                     └→ FixerClient, ChatClient (Ollama)
```

- Controllers implement `*Api` interfaces in `web/api/` (OpenAPI annotations live on the interface, not the controller class).
- Services own `@Transactional` boundaries and DTO mapping.
- Entities stay in `domain/`; API returns record DTOs from `web/dto/` only.

### Key services (do not duplicate logic elsewhere)

| Class | Responsibility |
|-------|----------------|
| `RateCalculationService.compute()` | **Only** spread-adjusted formula implementation |
| `CurrencySpreadProvider` | Appendix B spread percentages |
| `ExchangeRateService` | Exchange lookup, usage increment, **`historicalAdjusted()`** shared by history + insight |
| `RateIngestionService` | Fixer fetch + upsert; range backfill; insert-if-absent |
| `SampleRateSeedService` | Demo rates from `data/sample-rates.json` when no Fixer key |
| `TrendInsightService` | Spring AI prompt + deterministic fallback |
| `CurrencyUsageRepository.upsertIncrement()` | Atomic usage counters |

---

## Hard constraints (non-negotiable)

### Money

- All rates and computed results: **`java.math.BigDecimal`**. No `double`, `float`, or raw `Number`.
- DB: `numeric(19, 8)`. Computation: `MathContext.DECIMAL64`. JSON output: `setScale(10, HALF_UP)` via `RateCalculationService.RESULT_SCALE`.
- Formula (do not reimplement inline):

  ```
  adjustedRate = (toRateToUsd / fromRateToUsd) × ((100 − max(fromSpread, toSpread)) / 100)
  ```

### Dates

- `rate_date` on `ExchangeRate` = **Fixer API response `date`** — never `LocalDate.now()`.
- API date params: `LocalDate` + `@DateTimeFormat(iso = ISO.DATE)`.
- Scheduler cron: **`zone = "GMT"`** (not UTC).

### Concurrency

- Usage counters: **`CurrencyUsageRepository.upsertIncrement()`** — native SQL `INSERT … ON CONFLICT DO UPDATE SET query_count = query_count + 1`.
- **Never** use `AtomicLong`, `synchronized`, or read-modify-write in Java for counters.
- Verified in `CurrencyUsageRepositoryConcurrentIT`.

### Duplication

- Spread-adjusted time series: **only** `ExchangeRateService.historicalAdjusted()`. Historical and insight paths delegate to it.

### Spring AI

- `TrendInsightService` uses `ObjectProvider<ChatClient>` — handle `null` with deterministic fallback.
- Pass rate series as **JSON in the user message**; do not paraphrase numbers in prose for the LLM.
- **Ollama only** — one `ChatClient` bean in `AiConfig`; no OpenAI dual-provider toggle.

---

## REST API (no `/api` prefix)

| Method | Path | Notes |
|--------|------|-------|
| GET | `/exchange` | Appendix A; increments counters |
| GET | `/exchange/history` | Adjusted rate series |
| GET | `/exchange/insight` | LLM insight; Appendix A shape |
| GET | `/analytics` | `topCurrencies[]` |
| POST | `/admin/refresh` | Optional `from`/`to` for Fixer backfill |

Errors → RFC 7807 `ProblemDetail` via `GlobalExceptionHandler`.

---

## Backend conventions

- Constructor injection only; `@RequiredArgsConstructor` + `final` fields.
- JPA entities: Lombok getter/setter + protected no-arg ctor. DTOs: records in `web/dto/`.
- Custom queries: `@Query` with **named parameters** (`:currency`).
- `@Transactional` on writes; `@Transactional(readOnly = true)` on reads.
- Logging: SLF4J only; parameterised messages; `log.warn` for Fixer/LLM failures.
- JavaDoc: one short sentence per class/method; no rubric/brief references in source comments.

---

## Frontend conventions

- **Standalone components only** — no NgModules.
- **`ViewState<T>`** (`idle | loading | data | error`) + `signal<ViewState<T>>()` for every async feature.
- **`ApiService`** is the only `HttpClient` caller; models in `core/models/exchange.models.ts`.
- API base URL: `environment.ts` / Docker `runtime-config.json` — never hard-code `localhost:8080` in components.
- Reactive forms; cross-field validation in the component class.
- Charts: `ng2-charts` config stays in the feature component file.
- New route → update **component + `app.routes.ts` + `app.component.html` nav** in one change.

---

## Persistence & migrations

- **Flyway** (`db/migration/V*.sql`) — not Liquibase.
- Rates upserted via `ExchangeRateUpsertRepository` (`ON CONFLICT` on `(currency_code, rate_date)`).
- Fixer EUR-base quotes normalised to USD-per-unit in `RateToUsdNormalizer` on ingest.

---

## Testing expectations

| Test | Purpose |
|------|---------|
| `RateCalculationServiceTest` | §6.2 worked example (4.44) + Appendix B |
| `CurrencySpreadProviderTest` | Spread groups |
| `ExchangeControllerIT` | Testcontainers + `/exchange` + counters |
| `CurrencyUsageRepositoryConcurrentIT` | Concurrent counter safety |
| `RateIngestionServiceTest` | Ingestion + backfill behaviour |

Do not chase 90 % coverage — rubric wants spread logic + one integration path.

---

## Commits

- `[AI] feat|test|chore|docs: …` — agent produced the bulk of the diff.
- Plain Conventional Commits — human overrides and corrections (reviewers use this to spot judgment calls).

---

## Known overrides (do not regress)

1. **Counters:** SQL upsert, not `AtomicLong`.
2. **History + insight:** shared `historicalAdjusted()`, not duplicated controller logic.
3. **Sample data:** `data/sample-rates.json` + `SampleRateSeedService`, not hardcoded Java maps.
4. **Calculator form:** `nonNullable` group for currency fields; nullable `Date` for optional datepicker.

---

## Out of scope (YAGNI)

Authentication, Redis/cache, OpenAI provider, RAG/vector DB, Kubernetes manifests, metrics stack, fine-tuned models.

---

## Further reading

- [`README.md`](./README.md) — setup, AI workflow narrative, submission checklist
- [`PLAN.md`](./PLAN.md) — phased implementation plan
- [`.cursor/1-project.md`](./.cursor/1-project.md) — repo-wide agent rules (Cursor)
- [`.cursor/2-be-java.md`](./.cursor/2-be-java.md) — backend-scoped rules
- [`.cursor/3-fe-angular.md`](./.cursor/3-fe-angular.md) — frontend-scoped rules
