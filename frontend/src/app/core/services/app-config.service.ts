import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

interface RuntimeConfig {
  apiBaseUrl?: string;
}

/**
 * Loads optional runtime-config.json (Docker entrypoint) with fallback to environment.ts.
 */
@Injectable({ providedIn: 'root' })
export class AppConfigService {
  private apiBaseUrl = environment.apiBaseUrl;

  async load(): Promise<void> {
    try {
      const response = await fetch('/assets/runtime-config.json', { cache: 'no-store' });
      if (!response.ok) {
        return;
      }
      const config = (await response.json()) as RuntimeConfig;
      if (config.apiBaseUrl) {
        this.apiBaseUrl = config.apiBaseUrl;
      }
    } catch {
      // ng serve / missing file: keep environment default
    }
  }

  getApiBaseUrl(): string {
    return this.apiBaseUrl;
  }
}
