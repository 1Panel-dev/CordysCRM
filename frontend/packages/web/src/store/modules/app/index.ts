import { defineStore } from 'pinia';

import { setLocalStorage } from '@lib/shared/method/local-storage';

import { getKey } from '@/api/modules/system/login';
import { getModuleNavConfigList } from '@/api/modules/system/module';
import { useI18n } from '@/hooks/useI18n';
import { getThemeOverrides } from '@/utils/themeOverrides';

import type { AppState, PageConfig, Style, Theme } from './types';

const defaultThemeConfig = {
  style: 'default' as Style,
  customStyle: '#f9fbfb',
  theme: 'default' as Theme,
  customTheme: '#008d91',
};
const defaultLoginConfig = {
  title: 'Cordys',
  icon: [],
  loginLogo: [],
  loginImage: [],
  slogan: 'login.form.title',
};
const defaultPlatformConfig = {
  logoPlatform: [],
  platformName: 'Cordys',
  helpDoc: 'https://metersphere.io/docs/v3.x/',
};

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    menuCollapsed: false,
    collapsedWidth: 56,
    // 分页
    pageSize: 10,
    showSizePicker: true,
    showQuickJumper: true,
    loginLoading: false,
    loading: false,
    loadingTip: '',
    defaultThemeConfig,
    defaultLoginConfig,
    defaultPlatformConfig,
    themeOverridesConfig: getThemeOverrides(),
    pageConfig: {
      ...defaultThemeConfig,
      ...defaultLoginConfig,
      ...defaultPlatformConfig,
    },
    orgId: '',
    moduleConfigList: [],
  }),
  getters: {
    getMenuCollapsed(state: AppState) {
      return state.menuCollapsed;
    },
    getLoginLoadingStatus(state: AppState): boolean {
      return state.loginLoading;
    },
    getDefaultPageConfig(state: AppState): PageConfig {
      return {
        ...state.defaultThemeConfig,
        ...state.defaultLoginConfig,
        ...state.defaultPlatformConfig,
      };
    },
    getOrgId(state: AppState) {
      return state.orgId;
    },
  },
  actions: {
    setOrgId(id: string) {
      this.orgId = id;
    },
    setMenuCollapsed(collapsed: boolean) {
      this.menuCollapsed = collapsed;
    },
    /**
     * 显示全局 loading
     */
    showLoading(tip = '') {
      const { t } = useI18n();
      this.loading = true;
      this.loadingTip = tip || t('message.loadingDefaultTip');
    },
    /**
     * 隐藏全局 loading
     */
    hideLoading() {
      const { t } = useI18n();
      this.loading = false;
      this.loadingTip = t('message.loadingDefaultTip');
    },
    setLoginLoading(value: boolean) {
      this.loginLoading = value;
    },
    async initPublicKey() {
      try {
        const res = await getKey();
        setLocalStorage('publicKey', res);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
    },
    resetThemeOverridesConfig() {
      this.themeOverridesConfig = getThemeOverrides();
    },
    /**
     * 初始化模块配置
     */
    async initModuleConfig() {
      try {
        this.moduleConfigList = await getModuleNavConfigList({ organizationId: this.orgId });
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
  },
  persist: {
    paths: [''],
  },
});

export default useAppStore;
