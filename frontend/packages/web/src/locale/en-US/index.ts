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
    'menu.workbench': 'Home',
    'menu.settings': 'Settings',
    'menu.collapsedSettings': 'System',
    'menu.settings.org': 'Organization',
    'menu.settings.permission': 'Roles',
    'menu.settings.moduleSetting': 'Module',
    'menu.opportunity': 'Opportunity',
    'menu.quotation': 'Quotation',
    'menu.collapsedOpportunity': 'Opportunity',
    'menu.collapsedProduct': 'Product',
    'menu.clue': 'Lead',
    'menu.customer': 'Account',
    'menu.contact': 'Contact',
    'menu.dashboard': 'Dashboard',
    'menu.agent': 'Agent',
    'menu.tender': 'Tender',
    'menu.settings.businessSetting': 'Enterprise',
    'menu.settings.license': 'License',
    'menu.settings.messageSetting': 'Notification',
    'menu.settings.log': 'Logs',
    'navbar.action.locale': 'Switch to English',
    ...sys,
    ...localeSettings,
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'en-US',
};
