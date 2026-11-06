import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import {
  provideTransloco,
  TranslocoLoader,
  Translation,
} from '@jsverse/transloco';

export const SUPPORTED_LANGUAGES = ['en', 'de'] as const;
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number];

@Injectable({ providedIn: 'root' })
export class TranslocoHttpLoader implements TranslocoLoader {
  private readonly http = inject(HttpClient);

  getTranslation(lang: string) {
    return this.http.get<Translation>(`/i18n/${lang}.json`);
  }
}

/** Root Transloco setup. Translation files are served from /i18n/*.json. */
export function provideBankOpsTransloco() {
  return provideTransloco({
    config: {
      availableLangs: [...SUPPORTED_LANGUAGES],
      defaultLang: 'en',
      fallbackLang: 'en',
      reRenderOnLangChange: true,
      prodMode: false,
    },
    loader: TranslocoHttpLoader,
  });
}
