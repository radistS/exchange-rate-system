import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { CalculatorComponent } from './calculator.component';

describe('CalculatorComponent', () => {
  let fixture: ComponentFixture<CalculatorComponent>;
  let component: CalculatorComponent;
  let api: jasmine.SpyObj<ApiService>;

  beforeEach(async () => {
    api = jasmine.createSpyObj('ApiService', ['getExchange']);
    await TestBed.configureTestingModule({
      imports: [CalculatorComponent, NoopAnimationsModule],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ApiService, useValue: api },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CalculatorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('does not call API when currency code is invalid', () => {
    component.form.patchValue({ from: 'EURO', to: 'PLN' });
    component.submit();
    expect(api.getExchange).not.toHaveBeenCalled();
    expect(component.form.controls.from.invalid).toBeTrue();
    expect(component.resultState().status).toBe('idle');
  });

  it('does not call API when required field is empty', () => {
    component.form.patchValue({ from: '', to: 'PLN' });
    component.submit();
    expect(api.getExchange).not.toHaveBeenCalled();
    expect(component.form.controls.from.hasError('required')).toBeTrue();
  });

  it('calls API with uppercased currencies on valid submit', () => {
    api.getExchange.and.returnValue(
      of({
        from: 'EUR',
        to: 'PLN',
        exchange: 4.44,
        date: '2024-03-15',
        fromQueryCount: 1,
        toQueryCount: 1,
      }),
    );
    component.form.patchValue({ from: 'eur', to: 'pln', date: '' });
    component.submit();
    expect(api.getExchange).toHaveBeenCalledWith('EUR', 'PLN', undefined);
    expect(component.resultState().status).toBe('data');
  });

  it('sets error state when API fails', () => {
    api.getExchange.and.returnValue(throwError(() => new Error('Rate not found')));
    component.form.patchValue({ from: 'EUR', to: 'GBP' });
    component.submit();
    expect(component.resultState().status).toBe('error');
    if (component.resultState().status === 'error') {
      expect(component.resultState().error).toBe('Rate not found');
    }
  });
});
