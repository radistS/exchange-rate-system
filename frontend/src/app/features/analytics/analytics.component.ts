import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { ApiService } from '../../core/services/api.service';
import { AnalyticsResponse } from '../../core/models/exchange.models';
import { ViewState, data, error, loading } from '../../shared/view-state';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [
    MatCardModule,
    MatProgressSpinnerModule,
    MatTableModule,
    BaseChartDirective,
    DatePipe,
  ],
  templateUrl: './analytics.component.html',
  styleUrl: './analytics.component.scss',
})
export class AnalyticsComponent implements OnInit {
  private readonly api = inject(ApiService);

  /** Loads GET /analytics on init (no user trigger). */
  readonly viewState = signal<ViewState<AnalyticsResponse>>(loading());
  readonly displayedColumns = ['currency', 'totalCount', 'lastQueried'];

  barChartData: ChartData<'bar'> = { labels: [], datasets: [] };
  readonly barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { display: false } },
  };

  ngOnInit(): void {
    this.api.getAnalytics().subscribe({
      next: (res) => {
        this.updateChart(res);
        this.viewState.set(data(res));
      },
      error: (err: Error) => this.viewState.set(error(err.message)),
    });
  }

  private updateChart(res: AnalyticsResponse): void {
    const top = res.topCurrencies.slice(0, 10);
    this.barChartData = {
      labels: top.map((c) => c.currency),
      datasets: [
        {
          label: 'Query count',
          data: top.map((c) => c.totalCount),
          backgroundColor: '#42a5f5',
        },
      ],
    };
  }
}
