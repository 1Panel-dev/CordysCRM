import common from './common';
import localeSettings from './settings';
import sys from './sys';
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
    'menu.settings': '系统',
    'menu.collapsedSettings': '系统',
    'menu.settings.org': '组织架构',
    'menu.settings.permission': '角色权限',
    'menu.settings.moduleSetting': '模块配置',
    'menu.opportunity': '商机',
    'menu.quotation': '报价',
    'menu.collapsedOpportunity': '商机',
    'menu.collapsedProduct': '产品',
    'menu.clue': '线索',
    'menu.customer': '客户',
    'menu.contact': '联系人',
    'menu.dashboard': '仪表板',
    'menu.agent': '智能体',
    'menu.tender': '标讯',
    'menu.settings.businessSetting': '企业设置',
    'menu.settings.license': 'License',
    'menu.settings.messageSetting': '消息设置',
    'menu.settings.log': '系统日志',
    'navbar.action.locale': '切换为中文',
    ...sys,
    ...localeSettings,
    ...result,
    ...common,
  },
  dayjsLocale,
  dayjsLocaleName: 'zh-CN',
};
