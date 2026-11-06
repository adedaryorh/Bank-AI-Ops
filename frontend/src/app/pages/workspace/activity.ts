import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { WorkspacePage, WorkspaceStatus, WorkspacePager } from './workspace-shared';
import { PageResponse } from '../../core/api/api.models';
import { AuditRow, Execution } from '../../core/api/workspace.models';

@Component({
  selector: 'app-activity',
  standalone: true,
  imports: [TranslocoPipe, WorkspaceStatus, CommonModule, MatButtonModule, WorkspacePager],
  templateUrl: './activity.html',
  styleUrl: './workspace.scss',
})
export class ActivityPage extends WorkspacePage {
  protected readonly audit = signal<PageResponse<AuditRow> | null>(null);
  protected readonly executions = signal<PageResponse<Execution> | null>(null);
  constructor() {
    super();
    void this.load();
  }
  protected load() {
    return this.run(async () => {
      if (this.has('ADMIN', 'AUDITOR')) this.audit.set(await this.api.audit());
      this.executions.set(await this.api.executions());
    });
  }
  protected auditPage(page: number) {
    void this.run(async () => {
      this.audit.set(await this.api.audit(page));
    });
  }
  protected executionPage(page: number) {
    void this.run(async () => {
      this.executions.set(await this.api.executions(page));
    });
  }
}
