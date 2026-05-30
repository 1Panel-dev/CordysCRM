import type { LocaleType } from '@lib/shared/types/global';

export const loadLocalePool: LocaleType[] = [];

export function setLoadLocalePool(cb: (lp: LocaleType[]) => void) {
  cb(loadLocalePool);
}
