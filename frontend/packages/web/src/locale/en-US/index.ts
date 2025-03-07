import common from './common';
import localeSettings from './settings';
import sys from './sys';
import dayjsLocale from 'dayjs/locale/en';

const _Cmodules: any = import.meta.glob('../../components/**/locale/en-US.ts', { eager: true });
const _Vmodules: any = import.meta.glob('../../views/**/locale/en-US.ts', { eager: true });
let result = {};
Object.keys(_Cmodules).forEach((key) => {
  const defaultModule = _Cmodules[key as any].default;
  if (!defaultModule) return;
  result = { ...result, ...defaultModule };
});
Object.keys(_Vmodules).forEach((key) => {
  const defaultModule = _Vmodules[key as any].default;
  if (!defaultModule) return;
  result = { ...result, ...defaultModule };
});

export default {
  message: {
    'menu.workbenchHome': 'Home page',
    'menu.settings': 'Settings',
    'menu.settings.org': 'Organization',
    'menu.settings.permission': 'Role',
    'menu.settings.moduleSetting': 'Module',
    'menu.opportunity': 'Opportunity',
    'menu.clue': 'Clue',
    'menu.customer': 'Customer',
    'menu.contact': 'Contract',
    'menu.settings.businessSetting': 'Enterprise',
    'menu.settings.log': 'Log',
    'navbar.action.locale': 'Switch to English',
    ...sys,
    ...localeSettings,
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'en-US',
};
