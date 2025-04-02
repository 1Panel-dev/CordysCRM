import { defineStore } from 'pinia';
import { cloneDeep } from 'lodash-es';

import { CloseMessageUrl, SubscribeMessageUrl } from '@lib/shared/api/requrls/system/message';
import { getSSE } from '@lib/shared/method/index';
import { setLocalStorage } from '@lib/shared/method/local-storage';

import { getKey } from '@/api/modules/system/login';
import { getModuleNavConfigList } from '@/api/modules/system/module';
import { useI18n } from '@/hooks/useI18n';
import useUserStore from '@/store/modules/user';
import { getThemeOverrides } from '@/utils/themeOverrides';

import type { AppState, PageConfig, Style, Theme } from './types';
import type { RouteRecordRaw } from 'vue-router';

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
    currentTopMenu: {} as RouteRecordRaw,
    topMenus: [],
    messageInfo: {
      read: false,
      notificationDTOList: [],
      announcementDTOList: [],
    },
    eventSource: null,
    menuIconStatus: true,
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
    getTopMenus(state: AppState): RouteRecordRaw[] {
      return state.topMenus;
    },
    getCurrentTopMenu(state: AppState): RouteRecordRaw {
      return state.currentTopMenu;
    },
    getMenuIconStatus(state: AppState) {
      return state.menuIconStatus;
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
    /**
     * 设置顶部菜单组
     */
    setTopMenus(menus: RouteRecordRaw[] | undefined) {
      this.topMenus = menus ? [...menus] : [];
    },
    /**
     * 设置激活的顶部菜单
     */
    setCurrentTopMenu(menu: RouteRecordRaw) {
      this.currentTopMenu = cloneDeep(menu);
    },
    /**
     * 连接SSE消息订阅流
     */
    connectSystemMessageSSE(callback: () => void) {
      const userStore = useUserStore();

      if (this.eventSource) {
        this.eventSource.close();
      }

      this.eventSource = getSSE(SubscribeMessageUrl, {
        clientId: userStore.clientIdRandomId,
        userId: userStore.userInfo.id,
      });

      if (this.eventSource) {
        this.eventSource.onmessage = (event: MessageEvent) => {
          try {
            const data = JSON.parse(event.data);
            if (data.type === 'SYSTEM_HEARTBEAT') {
              return;
            }

            this.messageInfo = { ...data };
            callback();
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error('SSE Message parsing failure:', error);
          }
        };

        this.eventSource.onerror = () => {
          this.eventSource?.close();
          setTimeout(() => this.connectSystemMessageSSE(callback), 5000);
        };
      }
    },
    /**
     * 客户端主动断开连接
     */
    disconnectSystemMessageSSE() {
      const userStore = useUserStore();
      this.eventSource = getSSE(CloseMessageUrl, {
        clientId: userStore.clientIdRandomId,
        userId: userStore.userInfo.id,
      });
      this.eventSource.close();
      this.eventSource = null;
    },
    /**
     * 设置菜单icon展示
     */
    setMenuIconStatus(val: boolean) {
      const userStore = useUserStore();
      const storageData = JSON.parse(localStorage.getItem('MENU_ICON_SHOW') || '{}');

      storageData[userStore.userInfo.id] = val;
      this.menuIconStatus = val;
      localStorage.setItem('MENU_ICON_SHOW', JSON.stringify(storageData));
    },
  },
  persist: {
    paths: [''],
  },
});

export default useAppStore;
