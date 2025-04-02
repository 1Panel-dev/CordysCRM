import common from './common';
import dayjsLocale from 'dayjs/locale/zh-cn';

const _Cmodules: any = import.meta.glob('../../components/**/locale/zh-CN.ts', { eager: true });
const _Vmodules: any = import.meta.glob('../../views/**/locale/zh-CN.ts', { eager: true });
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
    'menu.workbench': '首页',
    'menu.duplicateCheck': '查重',
    'menu.opportunity': '商机',
    'menu.clue': '线索',
    'menu.cluePool': '线索池',
    'menu.customer': '客户',
    'menu.contact': '联系人',
    'menu.openSea': '公海',
    'menu.mine': '我的',
    'navbar.action.locale': '切换为中文',
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'zh-CN',
};
