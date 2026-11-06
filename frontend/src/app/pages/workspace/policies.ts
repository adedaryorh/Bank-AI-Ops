import { Component, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { TranslocoPipe } from '@jsverse/transloco';
import { RouterLink } from '@angular/router';
import { WorkspacePage, WorkspaceStatus } from './workspace-shared';
import { PolicyAnswer } from '../../core/api/workspace.models';

@Component({
  selector: 'app-policies',
  standalone: true,
  imports: [
    TranslocoPipe,
    WorkspaceStatus,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    RouterLink,
  ],
  templateUrl: './policies.html',
  styleUrl: './workspace.scss',
})
export class PoliciesPage extends WorkspacePage {
  private readonly fb = inject(FormBuilder);
  protected readonly form = this.fb.nonNullable.group({
    question: ['', [Validators.required, Validators.maxLength(1500)]],
  });
  protected readonly answer = signal<PolicyAnswer | null>(null);
  protected ask() {
    if (this.form.invalid) return;
    void this.run(async () => {
      this.answer.set(await this.api.ask(this.form.getRawValue().question));
    });
  }
  protected download(id: string, filename: string) {
    void this.run(() => this.api.download(id, filename));
  }
}
