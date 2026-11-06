import { DOCUMENT } from '@angular/common';
import { effect, inject, Injectable, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark';


@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);
  private readonly _mode = signal<ThemeMode>(this.initialMode());

  readonly mode = this._mode.asReadonly();

  constructor() {
    effect(() => {
      const mode = this._mode();
      const root = this.document.documentElement;
      root.classList.toggle('dark-theme', mode === 'dark');
      root.style.colorScheme = mode;
    });
  }

  toggle(): void {
    this._mode.update((mode) => (mode === 'light' ? 'dark' : 'light'));
  }

  private initialMode(): ThemeMode {
    try {
      return globalThis.matchMedia?.('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    } catch {
      return 'light';
    }
  }
}
