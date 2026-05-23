import { DecimalPipe } from '@angular/common';
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
import { ApiService } from '../../core/services/api.service';
import { ExchangeResponse } from '../../core/models/exchange.models';
import { toApiDate } from '../../shared/date.util';
import { ViewState, data, error, idle, loading } from '../../shared/view-state';

@Component({
  selector: 'app-calculator',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatIconModule,
    DecimalPipe,
  ],
  templateUrl: './calculator.component.html',
  styleUrl: './calculator.component.scss',
})
export class CalculatorComponent {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly resultState = signal<ViewState<ExchangeResponse>>(idle());

  readonly form = this.fb.nonNullable.group({
    from: ['EUR', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    to: ['PLN', [Validators.required, Validators.pattern(/^[A-Za-z]{3}$/)]],
    date: this.fb.control<Date | null>(null),
  });

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { from, to, date } = this.form.getRawValue();
    this.resultState.set(loading());
    this.api
      .getExchange(from.toUpperCase(), to.toUpperCase(), toApiDate(date) || undefined)
      .subscribe({
        next: (res) => this.resultState.set(data(res)),
        error: (err: Error) => this.resultState.set(error(err.message)),
      });
  }
}
