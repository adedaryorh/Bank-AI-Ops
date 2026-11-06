import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { RUNTIME_CONFIG } from '../config/runtime-config';
import { PageResponse } from './api.models';
import {
  CaseItem,
  CaseDetail,
  DocumentItem,
  PolicyAnswer,
  AmlAlert,
  Simulation,
  AuditRow,
  Execution,
  SyntheticTransaction,
} from './workspace.models';
@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly http = inject(HttpClient);
  private readonly base = inject(RUNTIME_CONFIG).apiBaseUrl;
  cases(page = 0, reviewQueue = false) {
    return firstValueFrom(
      this.http.get<PageResponse<CaseItem>>(`${this.base}/cases`, {
        params: { page, size: 20, reviewQueue },
      }),
    );
  }
  case(id: string) {
    return firstValueFrom(
      this.http.get<CaseDetail>(`${this.base}/cases/${encodeURIComponent(id)}`),
    );
  }
  createCase(input: {
    title: string;
    kind: string;
    description: string;
    reference: string;
    synthetic: boolean;
  }) {
    return firstValueFrom(this.http.post<CaseItem>(`${this.base}/cases`, input));
  }
  changeCase(id: string, action: 'notes' | 'submit', version: number, note: string) {
    return firstValueFrom(
      this.http.post<CaseDetail>(`${this.base}/cases/${encodeURIComponent(id)}/${action}`, {
        version,
        note,
      }),
    );
  }
  reviewCase(id: string, version: number, decision: 'CLOSE' | 'RETURN', reason: string) {
    return firstValueFrom(
      this.http.post<CaseDetail>(`${this.base}/cases/${encodeURIComponent(id)}/reviews`, {
        version,
        decision,
        reason,
      }),
    );
  }
  documents(page = 0, purpose?: string, caseId?: string) {
    let params = new HttpParams().set('page', page).set('size', 20);
    if (purpose) params = params.set('purpose', purpose);
    if (caseId) params = params.set('caseId', caseId);
    return firstValueFrom(
      this.http.get<PageResponse<DocumentItem>>(`${this.base}/documents`, { params }),
    );
  }
  upload(file: File, purpose: string, caseId: string, synthetic: boolean) {
    const body = new FormData();
    body.append('file', file);
    body.append('purpose', purpose);
    body.append('synthetic', String(synthetic));
    if (purpose === 'CASE') body.append('caseId', caseId);
    return firstValueFrom(this.http.post<DocumentItem>(`${this.base}/documents`, body));
  }
  async download(id: string, filename: string): Promise<void> {
    const data = await firstValueFrom(
      this.http.get(`${this.base}/documents/${encodeURIComponent(id)}/download`, {
        responseType: 'blob',
      }),
    );
    const url = URL.createObjectURL(data);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  ask(question: string) {
    return firstValueFrom(
      this.http.post<PolicyAnswer>(`${this.base}/policies/questions`, { question }),
    );
  }
  alerts(page = 0) {
    return firstValueFrom(
      this.http.get<PageResponse<AmlAlert>>(`${this.base}/aml/alerts`, {
        params: { page, size: 20 },
      }),
    );
  }
  amlEvidence(id: string) {
    return firstValueFrom(
      this.http.get<SyntheticTransaction[]>(
        `${this.base}/aml/cases/${encodeURIComponent(id)}/transactions`,
      ),
    );
  }
  simulate() {
    return firstValueFrom(this.http.post<Simulation>(`${this.base}/aml/simulations`, {}));
  }
  audit(page = 0) {
    return firstValueFrom(
      this.http.get<PageResponse<AuditRow>>(`${this.base}/audit/events`, {
        params: { page, size: 20 },
      }),
    );
  }
  executions(page = 0) {
    return firstValueFrom(
      this.http.get<PageResponse<Execution>>(`${this.base}/ai/executions`, {
        params: { page, size: 20 },
      }),
    );
  }
}
