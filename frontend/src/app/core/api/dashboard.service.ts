import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { RUNTIME_CONFIG } from '../config/runtime-config';
import { DashboardSummary } from './api.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly config = inject(RUNTIME_CONFIG);

  getSummary(): Observable<DashboardSummary> {
    return this.http.get<DashboardSummary>(`${this.config.apiBaseUrl}/dashboard/summary`);
  }
}
