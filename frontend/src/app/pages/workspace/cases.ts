import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TranslocoPipe } from '@jsverse/transloco';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { WorkspacePage, WorkspaceStatus, WorkspacePager } from './workspace-shared';
import { PageResponse } from '../../core/api/api.models';
import { CaseItem } from '../../core/api/workspace.models';

@Component({
  selector: 'app-cases',
  standalone: true,
  imports: [
    TranslocoPipe,
    WorkspaceStatus,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    RouterLink,
    WorkspacePager,
  ],
  templateUrl: './cases.html',
  styleUrl: './workspace.scss',
})
export class CasesPage extends WorkspacePage {
  protected readonly result = signal<PageResponse<CaseItem> | null>(null);
  private readonly route = inject(ActivatedRoute);
  protected readonly reviewOnly = signal(this.route.snapshot.data['reviewQueue'] === true);
  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(180)]],
    kind: ['OPERATIONS', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(10000)]],
    reference: ['SYN-', [Validators.required, Validators.pattern(/^SYN-[A-Za-z0-9-]{1,76}$/)]],
    synthetic: [false, Validators.requiredTrue],
  });
  constructor() {
    super();
    void this.load();
  }
  protected load(page = 0) {
    return this.run(async () => {
      this.result.set(await this.api.cases(page, this.reviewOnly()));
    });
  }
  protected filter(checked: boolean) {
    this.reviewOnly.set(checked);
    void this.load();
  }
  protected create() {
    if (this.form.invalid) return;
    void this.run(async () => {
      await this.api.createCase(this.form.getRawValue());
      this.form.reset({ kind: 'OPERATIONS', reference: 'SYN-', synthetic: false });
      this.result.set(await this.api.cases(0, this.reviewOnly()));
    });
  }
}
