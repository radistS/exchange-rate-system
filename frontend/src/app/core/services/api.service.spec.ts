import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AppConfigService } from './app-config.service';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;
  const baseUrl = 'http://test-api.local';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ApiService,
        {
          provide: AppConfigService,
          useValue: { getApiBaseUrl: () => baseUrl },
        },
      ],
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getExchange calls /exchange with required params', async () => {
    const promise = firstValueFrom(service.getExchange('EUR', 'PLN', '2024-03-15'));
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${baseUrl}/exchange` &&
        r.params.get('from') === 'EUR' &&
        r.params.get('to') === 'PLN' &&
        r.params.get('date') === '2024-03-15',
    );
    expect(req.request.method).toBe('GET');
    req.flush({
      from: 'EUR',
      to: 'PLN',
      exchange: 4.44,
      date: '2024-03-15',
      fromQueryCount: 1,
      toQueryCount: 1,
    });
    const result = await promise;
    expect(result.exchange).toBe(4.44);
  });

  it('getExchange omits date param when not provided', async () => {
    const promise = firstValueFrom(service.getExchange('EUR', 'PLN'));
    const req = httpMock.expectOne(
      (r) =>
        r.url === `${baseUrl}/exchange` &&
        r.params.get('from') === 'EUR' &&
        r.params.get('to') === 'PLN' &&
        !r.params.has('date'),
    );
    req.flush({
      from: 'EUR',
      to: 'PLN',
      exchange: 4.44,
      date: '2024-03-15',
      fromQueryCount: 0,
      toQueryCount: 0,
    });
    await promise;
  });

  it('maps ProblemDetail errors to Error with detail message', async () => {
    const promise = firstValueFrom(service.getExchange('EUR', 'GBP'));
    const req = httpMock.expectOne(
      (r) => r.url === `${baseUrl}/exchange` && r.params.get('from') === 'EUR' && r.params.get('to') === 'GBP',
    );
    req.flush(
      { title: 'Rate not found', detail: 'No rate for currency GBP on date 2024-03-15', status: 404 },
      { status: 404, statusText: 'Not Found' },
    );
    await expectAsync(promise).toBeRejectedWithError('No rate for currency GBP on date 2024-03-15');
  });

  it('getAnalytics calls /analytics', async () => {
    const promise = firstValueFrom(service.getAnalytics());
    const req = httpMock.expectOne(`${baseUrl}/analytics`);
    expect(req.request.method).toBe('GET');
    req.flush({ topCurrencies: [{ currency: 'EUR', totalCount: 5, lastQueried: '2024-03-15' }] });
    const result = await promise;
    expect(result.topCurrencies.length).toBe(1);
  });
});
