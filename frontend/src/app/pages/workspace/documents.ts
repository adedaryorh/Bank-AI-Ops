import { CommonModule } from '@angular/common';
import { Component, inject, signal, DestroyRef } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TranslocoPipe } from '@jsverse/transloco';
import { ActivatedRoute } from '@angular/router';
import { WorkspacePage, WorkspaceStatus, WorkspacePager } from './workspace-shared';
import { PageResponse } from '../../core/api/api.models';
import { DocumentItem } from '../../core/api/workspace.models';

@Component({
  selector: 'app-documents',
  standalone: true,
  imports: [
    TranslocoPipe,
    WorkspaceStatus,
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    WorkspacePager,
  ],
  templateUrl: './documents.html',
  styleUrl: './workspace.scss',
})
export class DocumentsPage extends WorkspacePage {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly result = signal<PageResponse<DocumentItem> | null>(null);
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly form = this.fb.nonNullable.group({
    purpose: [this.route.snapshot.queryParamMap.has('caseId') ? 'CASE' : 'POLICY'],
    caseId: [this.route.snapshot.queryParamMap.get('caseId') ?? ''],
    synthetic: [false, Validators.requiredTrue],
  });
  constructor() {
    super();
    void this.load();
    const timer = setInterval(() => {
      if (this.result()?.content.some((d) => ['QUEUED', 'PROCESSING'].includes(d.status)))
        void this.load(this.result()?.page ?? 0);
    }, 3000);
    this.destroyRef.onDestroy(() => clearInterval(timer));
  }
  protected load(page = 0) {
    return this.run(async () => {
      const v = this.form.getRawValue();
      this.result.set(
        await this.api.documents(page, v.purpose, v.purpose === 'CASE' ? v.caseId : undefined),
      );
    });
  }
  protected canUpload() {
    const v = this.form.getRawValue();
    return v.purpose === 'POLICY'
      ? this.has('ADMIN', 'COMPLIANCE_ANALYST')
      : this.has('ADMIN', 'OPERATIONS_OFFICER', 'COMPLIANCE_ANALYST', 'AML_ANALYST', 'SUPERVISOR');
  }
  protected choose(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0] ?? null;
    this.selectedFile.set(file);
  }
  protected upload() {
    const file = this.selectedFile();
    const v = this.form.getRawValue();
    if (!file || !v.synthetic) return;
    void this.run(async () => {
      await this.api.upload(file, v.purpose, v.caseId, v.synthetic);
      this.form.controls.synthetic.setValue(false);
      this.result.set(
        await this.api.documents(0, v.purpose, v.purpose === 'CASE' ? v.caseId : undefined),
      );
    });
  }
  protected download(doc: DocumentItem) {
    void this.run(() => this.api.download(doc.id, doc.filename));
  }
  protected fields(doc: DocumentItem) {
    return Object.entries(doc.extractedFields ?? {});
  }
}
