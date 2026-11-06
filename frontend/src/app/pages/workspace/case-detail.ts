import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslocoPipe } from '@jsverse/transloco';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { WorkspacePage, WorkspaceStatus } from './workspace-shared';
import { CaseDetail, SyntheticTransaction } from '../../core/api/workspace.models';

@Component({
  selector: 'app-case-detail',
  standalone: true,
  imports: [
    TranslocoPipe,
    WorkspaceStatus,
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
  ],
  templateUrl: './case-detail.html',
  styleUrl: './workspace.scss',
})
export class CaseDetailPage extends WorkspacePage {
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  protected readonly transactions = signal<SyntheticTransaction[]>([]);
  protected readonly detail = signal<CaseDetail | null>(null);
  protected readonly noteForm = this.fb.nonNullable.group({
    note: ['', [Validators.required, Validators.maxLength(4000)]],
  });
  protected readonly reviewForm = this.fb.nonNullable.group({
    reason: ['', [Validators.required, Validators.minLength(10), Validators.maxLength(4000)]],
  });
  private readonly id = this.route.snapshot.paramMap.get('id') ?? '';
  constructor() {
    super();
    void this.load();
  }
  protected load() {
    return this.run(async () => {
      this.detail.set(await this.api.case(this.id));
      if (
        this.detail()?.item.kind === 'AML' &&
        this.has('ADMIN', 'AML_ANALYST', 'SUPERVISOR', 'AUDITOR')
      )
        this.transactions.set(await this.api.amlEvidence(this.id));
    });
  }
  protected editable() {
    return (
      ['OPEN', 'RETURNED'].includes(this.detail()?.item.status ?? '') &&
      this.has('ADMIN', 'OPERATIONS_OFFICER', 'COMPLIANCE_ANALYST', 'AML_ANALYST', 'SUPERVISOR')
    );
  }
  protected change(action: 'notes' | 'submit') {
    const item = this.detail()?.item;
    if (!item || this.noteForm.invalid) return;
    void this.run(async () => {
      this.detail.set(
        await this.api.changeCase(item.id, action, item.version, this.noteForm.getRawValue().note),
      );
      this.noteForm.reset();
    });
  }
  protected review(decision: 'CLOSE' | 'RETURN') {
    const item = this.detail()?.item;
    if (!item || this.reviewForm.invalid) return;
    void this.run(async () => {
      this.detail.set(
        await this.api.reviewCase(
          item.id,
          item.version,
          decision,
          this.reviewForm.getRawValue().reason,
        ),
      );
      this.reviewForm.reset();
    });
  }
}
