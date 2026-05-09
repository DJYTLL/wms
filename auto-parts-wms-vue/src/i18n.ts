import { createI18n } from 'vue-i18n';

type Locale = 'en' | 'zh';
type MessageSchema = Record<string, unknown>;

const DEFAULT_LOCALE: Locale = 'en';

const resolveStoredLocale = (): Locale => {
  if (typeof localStorage === 'undefined') {
    return DEFAULT_LOCALE;
  }
  const stored = localStorage.getItem('locale');
  return stored === 'zh' ? 'zh' : DEFAULT_LOCALE;
};

const currentLocale = resolveStoredLocale();

const messageLoaders: Record<Locale, () => Promise<MessageSchema>> = {
  en: () => import('./locales/en').then((module) => module.default),
  zh: () => import('./locales/zh').then((module) => module.default),
};

const loadedLocales = new Set<Locale>();

const i18n = createI18n({
  legacy: false,
  locale: currentLocale,
  fallbackLocale: DEFAULT_LOCALE,
  messages: {},
});

const loadLocaleMessages = async (locale: Locale) => {
  if (loadedLocales.has(locale)) {
    return;
  }
  const messages = await messageLoaders[locale]();
  i18n.global.setLocaleMessage(locale, messages);
  loadedLocales.add(locale);
};

export const setLocale = async (locale: Locale) => {
  await loadLocaleMessages(locale);
  i18n.global.locale.value = locale;
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem('locale', locale);
  }
};

export const setupI18n = async () => {
  await loadLocaleMessages(DEFAULT_LOCALE);
  if (currentLocale !== DEFAULT_LOCALE) {
    await loadLocaleMessages(currentLocale);
  }
  i18n.global.locale.value = currentLocale;
  return i18n;
};

export default i18n;
