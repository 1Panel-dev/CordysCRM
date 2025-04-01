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
    'menu.workbench': 'Home page',
    'menu.opportunity': 'Opportunity',
    'menu.clue': 'Clue',
    'menu.customer': 'Customer',
    'menu.contact': 'Contact',
    'menu.mine': 'Mine',
    'navbar.action.locale': 'Switch to English',
    ...result,
  },
  dayjsLocale,
  dayjsLocaleName: 'en-US',
};
