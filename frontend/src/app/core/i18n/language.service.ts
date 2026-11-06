import { DOCUMENT } from '@angular/common';
import { DestroyRef, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslocoService } from '@jsverse/transloco';
import { SUPPORTED_LANGUAGES, SupportedLanguage } from './transloco';


const LANGUAGE_STORAGE_KEY = 'bankops.language';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly transloco = inject(TranslocoService);
  private readonly document = inject(DOCUMENT);
  private readonly destroyRef = inject(DestroyRef);

  readonly activeLang = signal<SupportedLanguage>(this.transloco.getActiveLang() as SupportedLanguage);

  constructor() {
    const preferred = this.readPreference();
    if (preferred) {
      this.transloco.setActiveLang(preferred);
    }
    this.activeLang.set(this.transloco.getActiveLang() as SupportedLanguage);
    this.applyDocumentLanguage(this.transloco.getActiveLang());

    this.transloco.langChanges$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((lang) => {
      this.activeLang.set(lang as SupportedLanguage);
      this.applyDocumentLanguage(lang);
    });
  }

  setLanguage(lang: string): void {
    if (!this.isSupported(lang)) {
      return;
    }
    this.transloco.setActiveLang(lang);
    this.persist(lang);
  }

  toggle(): void {
    this.setLanguage(this.activeLang() === 'en' ? 'de' : 'en');
  }

  isSupported(lang: string): lang is SupportedLanguage {
    return (SUPPORTED_LANGUAGES as readonly string[]).includes(lang);
  }

  private applyDocumentLanguage(lang: string): void {
    this.document.documentElement.lang = lang;
  }

  private readPreference(): SupportedLanguage | null {
    try {
      const stored = localStorage.getItem(LANGUAGE_STORAGE_KEY);
      return stored && this.isSupported(stored) ? stored : null;
    } catch {
      return null;
    }
  }

  private persist(lang: SupportedLanguage): void {
    try {
      localStorage.setItem(LANGUAGE_STORAGE_KEY, lang);
    } catch {
      // A blocked localStorage must not break the language switch.
    }
  }
}
