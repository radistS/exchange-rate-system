import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, throwError } from 'rxjs';
import { AppConfigService } from './app-config.service';
import {
  AnalyticsResponse,
  ExchangeResponse,
  HistoryResponse,
  InsightResponse,
  ProblemDetail,
} from '../models/exchange.models';

/**
 * Sole HttpClient entry point — components must not call the backend directly.
 * All methods map errors to a simple Error with a message (RFC 7807 ProblemDetail).
 */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly appConfig = inject(AppConfigService);

  private get baseUrl(): string {
    return this.appConfig.getApiBaseUrl();
  }

  getExchange(from: string, to: string, date?: string): Observable<ExchangeResponse> {
    let params = new HttpParams().set('from', from).set('to', to);
    if (date) {
      params = params.set('date', date);
    }
    return this.http
      .get<ExchangeResponse>(`${this.baseUrl}/exchange`, { params })
      .pipe(catchError((err) => this.normaliseError(err)));
  }

  getHistory(
    from: string,
    to: string,
    fromDate: string,
    toDate: string,
  ): Observable<HistoryResponse> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http
      .get<HistoryResponse>(`${this.baseUrl}/exchange/history`, { params })
      .pipe(catchError((err) => this.normaliseError(err)));
  }

  getInsight(
    from: string,
    to: string,
    fromDate: string,
    toDate: string,
  ): Observable<InsightResponse> {
    const params = new HttpParams()
      .set('from', from)
      .set('to', to)
      .set('fromDate', fromDate)
      .set('toDate', toDate);
    return this.http
      .get<InsightResponse>(`${this.baseUrl}/exchange/insight`, { params })
      .pipe(catchError((err) => this.normaliseError(err)));
  }

  getAnalytics(): Observable<AnalyticsResponse> {
    return this.http
      .get<AnalyticsResponse>(`${this.baseUrl}/analytics`)
      .pipe(catchError((err) => this.normaliseError(err)));
  }

  /** Aligns Spring {@code ProblemDetail} and plain-text error bodies for the UI. */
  private normaliseError(err: unknown): Observable<never> {
    if (err instanceof HttpErrorResponse) {
      const body = err.error as ProblemDetail | string | null;
      const detail =
        typeof body === 'string'
          ? body
          : body?.detail ?? body?.title ?? `Request failed (${err.status})`;
      return throwError(() => new Error(detail));
    }
    return throwError(() => (err instanceof Error ? err : new Error('Unexpected error')));
  }
}
