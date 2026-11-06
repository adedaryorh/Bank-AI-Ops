import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatDividerModule } from '@angular/material/divider';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { map } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { LanguageService } from '../i18n/language.service';
import { ThemeService } from '../theme/theme.service';
import { AppIcon } from '../../shared/icons/app-icon';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatButtonToggleModule,
    MatDividerModule,
    MatMenuModule,
    MatTooltipModule,
    TranslocoPipe,
    AppIcon,
  ],
  templateUrl: './app-shell.html',
  styleUrl: './app-shell.scss',
})
export class AppShell {
  protected readonly auth = inject(AuthService);
  protected readonly language = inject(LanguageService);
  protected readonly theme = inject(ThemeService);

  private readonly breakpoints = inject(BreakpointObserver);
  protected readonly isMobile = toSignal(
    this.breakpoints.observe('(max-width: 900px)').pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly collapsed = signal(false);
  protected readonly mobileOpen = signal(false);

  protected readonly displayName = computed(() => this.auth.user()?.displayName ?? '—');
  protected readonly initials = computed(() => {
    const name = this.auth.user()?.displayName?.trim();
    if (!name) {
      return '?';
    }
    return name
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0))
      .join('')
      .toUpperCase();
  });
  protected readonly department = computed(() => {
    const user = this.auth.user();
    return user?.departmentName ?? user?.department ?? '—';
  });
  protected readonly roles = computed(() => this.auth.user()?.roles ?? []);

  constructor() {
    this.breakpoints
      .observe('(max-width: 900px)')
      .pipe(takeUntilDestroyed())
      .subscribe((state) => {
        if (!state.matches) {
          this.mobileOpen.set(false);
        }
      });
  }

  protected toggleNavigation(): void {
    if (this.isMobile()) {
      this.mobileOpen.update((open) => !open);
    } else {
      this.collapsed.update((value) => !value);
    }
  }

  protected closeMobileNavigation(): void {
    this.mobileOpen.set(false);
  }

  protected setLanguage(value: MatButtonToggleChange['value']): void {
    if (typeof value === 'string') {
      this.language.setLanguage(value);
    }
  }

  protected logout(): void {
    void this.auth.logout();
  }
}
