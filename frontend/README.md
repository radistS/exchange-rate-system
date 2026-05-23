# Frontend

Angular 19 · standalone components · Material · reactive forms · ng2-charts.

## Run

```bash
npm install
npm start
```

API base URL (in order of precedence):

1. `src/assets/runtime-config.json` (overwritten in Docker from `API_URL`)
2. `src/environments/environment.ts` (`apiBaseUrl`)

## Routes

| Path | Feature |
|------|---------|
| `/calculator` | Spread-adjusted exchange rate |
| `/historical` | Table, line chart, AI insight |
| `/analytics` | Usage bar chart and table |
