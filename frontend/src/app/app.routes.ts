import { Routes } from '@angular/router';

/** Lazy-loaded feature routes (standalone components). */
export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'calculator',
    loadComponent: () =>
      import('./features/calculator/calculator.component').then((m) => m.CalculatorComponent),
  },
  {
    path: 'historical',
    loadComponent: () =>
      import('./features/historical/historical.component').then((m) => m.HistoricalComponent),
  },
  {
    path: 'analytics',
    loadComponent: () =>
      import('./features/analytics/analytics.component').then((m) => m.AnalyticsComponent),
  },
  { path: '**', redirectTo: 'calculator' },
];
