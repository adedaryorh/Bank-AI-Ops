import { Component, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { RouterLink } from '@angular/router';
import { WorkspacePage, WorkspaceStatus, WorkspacePager } from './workspace-shared';
import { PageResponse } from '../../core/api/api.models';
import { AmlAlert, Simulation } from '../../core/api/workspace.models';

@Component({
  selector: 'app-aml',
  standalone: true,
  imports: [TranslocoPipe, WorkspaceStatus, MatButtonModule, RouterLink, WorkspacePager],
  templateUrl: './aml.html',
  styleUrl: './workspace.scss',
})
export class AmlPage extends WorkspacePage {
  protected readonly result = signal<PageResponse<AmlAlert> | null>(null);
  protected readonly simulation = signal<Simulation | null>(null);
  constructor() {
    super();
    void this.load();
  }
  protected load(page = 0) {
    return this.run(async () => {
      this.result.set(await this.api.alerts(page));
    });
  }
  protected simulate() {
    void this.run(async () => {
      this.simulation.set(await this.api.simulate());
      this.result.set(await this.api.alerts());
    });
  }
}
