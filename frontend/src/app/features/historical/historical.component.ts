import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatNativeDateModule } from '@angular/material/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { ChartConfiguration, ChartData } from 'chart.js';
import { BaseChartDirective } from 'ng2-charts';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { HistoryResponse, InsightResponse } from '../../core/models/exchange.models';
import { defaultHistoricalRange, toApiDate } from '../../shared/date.util';
import { ViewState, data, error, idle, loading } from '../../shared/view-state';

interface HistoricalView {
  history: HistoryResponse;
  insight: InsightResponse;
}

@Component({
  selector: 'app-historical',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTableModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    BaseChartDirective,
    DatePipe,
    DecimalPipe,
  ],
  templateUrl: './historical.component.html',
  styleUrl: './historical.component.scss',
})
export class HistoricalComponent {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly viewState = signal<ViewState<HistoricalView>>(idle());
  readonly displayedColumns = ['date', 'rate'];

  lineChartData: ChartData<'line'> = { labels: [], datasets: [] };
  readonly lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    scales: { y: { beginAtZero: false } },
  };

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    to: ['PLN', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    fromDate: [defaultHistoricalRange().from, Validators.required],
    toDate: [defaultHistoricalRange().to, Validators.required],
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, fromDate, toDate } = this.form.getRawValue();
    const fromStr = toApiDate(fromDate);
    const toStr = toApiDate(toDate);
    if (toStr < fromStr) {
      this.viewState.set(error('End date must not be before start date.'));
      return;
    }
    const fromCode = from.toUpperCase();
    const toCode = to.toUpperCase();
    this.viewState.set(loading());
    forkJoin({
      history: this.api.getHistory(fromCode, toCode, fromStr, toStr),
      insight: this.api.getInsight(fromCode, toCode, fromStr, toStr),
    }).subscribe({
      next: (res) => {
        this.updateChart(res.history);
        this.viewState.set(data(res));
      },
      error: (err: Error) => this.viewState.set(error(err.message)),
    });
  }

  private updateChart(history: HistoryResponse): void {
    this.lineChartData = {
      labels: history.rates.map((p) => p.date),
      datasets: [
        {
          label: `${history.from}/${history.to}`,
          data: history.rates.map((p) => p.rate),
          fill: false,
          tension: 0.2,
          borderColor: '#1565c0',
        },
      ],
    };
  }
}
