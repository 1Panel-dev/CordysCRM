import common from './common';
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
    'menu.workbench': 'Home',
    'menu.duplicateCheck': 'Duplicate Check',
    'menu.opportunity': 'Opportunity',
    'menu.clue': 'Lead',
    'menu.cluePool': 'Lead Pool',
    'menu.customer': 'Account',
    'menu.openSea': 'Open Sea',
    'menu.contact': 'Contact',
    'menu.mine': 'Mine',
    'navbar.action.locale': 'Switch to English',
    ...sys,
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'en-US',
};
