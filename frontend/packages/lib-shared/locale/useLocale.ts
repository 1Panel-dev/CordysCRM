import { ref, unref } from 'vue';
import dayjs from 'dayjs';

import { loadLocalePool } from './helper';
import { i18n } from './index';
import type { LocaleType, Recordable } from '@lib/shared/types/global';

interface LangModule {
  message: Recordable;
  dayjsLocale: Recordable;
  dayjsLocaleName: string;
}

export default function useLocale(showLoadingTip: (message: string) => void) {
  const { locale } = i18n.global;
  const currentLocale = ref(locale as LocaleType);

  /**
   * 设置语言
   * @param _locale 语言类型
   */
  function setI18nLanguage(_locale: LocaleType) {
    if (i18n.mode === 'legacy') {
      i18n.global.locale = _locale;
    } else {
      (i18n.global.locale as any).value = _locale;
    }
    localStorage.setItem('CRM-locale', _locale);
  }

  /**
   * 切换语言
   * @param _locale 语言类型
   * @returns 语言类型
   */
  async function changeLocale(_locale: LocaleType) {
    const globalI18n = i18n.global;
    const _currentLocale = unref(globalI18n.locale);
    if (_currentLocale === _locale) {
      setI18nLanguage(_locale); // 初始化的时候需要设置一次本地语言
      return _locale;
    }

    showLoadingTip(_currentLocale === 'zh-CN' ? '语言切换中...' : 'Language switching...');

    if (loadLocalePool.includes(_locale)) {
      setI18nLanguage(_locale);
      return _locale;
    }
    const langModule = ((await import(`@locale/${_locale}/index.ts`)) as any).default as LangModule;
    if (!langModule) return;

    const { message, dayjsLocale, dayjsLocaleName } = langModule;

    globalI18n.setLocaleMessage(_locale, message);
    dayjs.locale(dayjsLocaleName, dayjsLocale);
    loadLocalePool.push(_locale);

    setI18nLanguage(_locale);
    window.location.reload();
    return _locale;
  }

  return {
    currentLocale,
    changeLocale,
  };
}
