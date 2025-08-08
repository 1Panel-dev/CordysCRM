import { defineStore } from 'pinia';
import { cloneDeep } from 'lodash-es';

import { SubscribeMessageUrl } from '@lib/shared/api/requrls/system/message';
import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import { ModuleConfigEnum } from '@lib/shared/enums/moduleEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getSSE } from '@lib/shared/method';
import { setLocalStorage } from '@lib/shared/method/local-storage';
import { loadScript } from '@lib/shared/method/scriptLoader';

import {
  closeMessageSubscribe,
  getHomeMessageList,
  getKey,
  getModuleNavConfigList,
  getSystemVersion,
  getThirdConfigByType,
  getUnReadAnnouncement,
} from '@/api/modules';
import useUserStore from '@/store/modules/user';
import { getThemeOverrides } from '@/utils/themeOverrides';

import useLicenseStore from '../setting/license';
import type { AppState, PageConfig, Style, Theme } from './types';
import type { RouteRecordRaw } from 'vue-router';

const defaultThemeConfig = {
  style: 'default' as Style,
  customStyle: '#f9fbfb',
  theme: 'default' as Theme,
  customTheme: '#008d91',
};
const defaultLoginConfig = {
  title: 'CORDYS',
  icon: [],
  loginLogo: [],
  loginImage: [],
  slogan: 'login.form.title',
};
const defaultPlatformConfig = {
  logoPlatform: [],
  platformName: 'CORDYS',
  helpDoc: 'https://metersphere.io/docs/v3.x/',
};

const defaultModuleConfig = [
  {
    moduleKey: ModuleConfigEnum.DASHBOARD,
    enable: true,
  },
  {
    moduleKey: ModuleConfigEnum.HOME,
    enable: true,
  },
  {
    moduleKey: ModuleConfigEnum.CUSTOMER_MANAGEMENT,
    enable: true,
  },
  {
    moduleKey: ModuleConfigEnum.CLUE_MANAGEMENT,
    enable: true,
  },
  {
    moduleKey: ModuleConfigEnum.BUSINESS_MANAGEMENT,
    enable: true,
  },
  {
    moduleKey: ModuleConfigEnum.PRODUCT_MANAGEMENT,
    enable: true,
  },
];

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    menuCollapsed: false,
    collapsedWidth: 56,
    // 分页
    pageSize: 30,
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
    moduleConfigList: cloneDeep(defaultModuleConfig),
    currentTopMenu: {} as RouteRecordRaw,
    topMenus: [],
    messageInfo: {
      read: true,
      notificationDTOList: [],
      announcementDTOList: [],
    },
    eventSource: null,
    menuIconStatus: {},
    restoreMenuTimeStamp: 0,
    version: '',
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
      const userId = useUserStore().userInfo.id;
      return state.menuIconStatus[userId] ?? true;
    },
    getRestoreMenuTimeStamp(state: AppState) {
      return state.restoreMenuTimeStamp;
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
        this.moduleConfigList = await getModuleNavConfigList({
          organizationId: this.orgId,
        });
        if (!useLicenseStore().hasLicense()) {
          this.moduleConfigList = this.moduleConfigList.filter((e) => e.moduleKey !== ModuleConfigEnum.DASHBOARD);
        }
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
    async connectSystemMessageSSE(callback: () => void) {
      const userStore = useUserStore();

      await this.disconnectSystemMessageSSE();
      this.eventSource = getSSE(SubscribeMessageUrl, {
        clientId: userStore.clientIdRandomId,
        userId: userStore.userInfo.id,
      });
      if (this.eventSource) {
        this.eventSource.onmessage = (event: MessageEvent) => {
          try {
            if (event.data && event.data.includes('HEARTBEAT')) {
              return;
            }

            const data = JSON.parse(event.data);

            this.messageInfo = { ...data };
            callback();
          } catch (error) {
            // eslint-disable-next-line no-console
            console.error('SSE Message parsing failure:', error);
          }
        };
        // 错误直接关闭，手动刷新
        this.eventSource.onerror = () => {
          if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
          }
        };
      }
    },
    /**
     * 客户端主动断开连接
     */
    async disconnectSystemMessageSSE() {
      const userStore = useUserStore();
      if (!userStore.clientIdRandomId || !userStore.userInfo.id) return;
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
      }
      try {
        await closeMessageSubscribe({ clientId: userStore.clientIdRandomId, userId: userStore.userInfo.id });
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    /**
     * 设置菜单icon展示
     */
    setMenuIconStatus(val: boolean) {
      const userStore = useUserStore();
      this.menuIconStatus[userStore.userInfo.id] = val;
    },
    /**
     * 初始化首页消息
     */
    async initMessage() {
      try {
        const [notifications, announcements] = await Promise.all([getHomeMessageList(), getUnReadAnnouncement()]);
        this.messageInfo.notificationDTOList = notifications;
        this.messageInfo.announcementDTOList = announcements;
        this.messageInfo.read = !(announcements?.length || notifications?.length);

        const userStore = useUserStore();
        userStore.showSystemNotify();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    setRestoreMenuTimeStamp(timeStamp: number) {
      this.restoreMenuTimeStamp = timeStamp;
    },
    async getVersion() {
      try {
        this.version = await getSystemVersion();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    // 显示 SQLBot
    async showSQLBot() {
      const licenseStore = useLicenseStore();
      if (!licenseStore.hasLicense()) return;
      const res = await getThirdConfigByType(CompanyTypeEnum.SQLBot);
      await loadScript(res.appSecret as string, { identifier: CompanyTypeEnum.SQLBot });
    },
  },
  persist: {
    paths: ['menuIconStatus', 'moduleConfigList'],
  },
});

export default useAppStore;
