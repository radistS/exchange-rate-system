/** Mirrors backend DTOs — keep in sync with OpenAPI / Java records. */
export interface ExchangeResponse {
  from: string;
  to: string;
  exchange: number;
  date: string;
  fromQueryCount: number;
  toQueryCount: number;
}

export interface RatePoint {
  date: string;
  rate: number;
}

export interface HistoryResponse {
  from: string;
  to: string;
  rates: RatePoint[];
}

export interface InsightResponse {
  from: string;
  to: string;
  fromDate: string;
  toDate: string;
  insight: string;
}

export interface CurrencyUsage {
  currency: string;
  totalCount: number;
  lastQueried: string | null;
}

export interface AnalyticsResponse {
  topCurrencies: CurrencyUsage[];
}

export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
}
