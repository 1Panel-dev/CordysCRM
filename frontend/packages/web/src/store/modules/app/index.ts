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
  getModuleTopNavList,
  getPageConfig,
  getSystemVersion,
  getThirdConfigByType,
  getUnReadAnnouncement,
} from '@/api/modules';
import { defaultNavList } from '@/config/system';
import useUserStore from '@/store/modules/user';
import { watchStyle, watchTheme } from '@/utils/theme';
import { getThemeOverrides } from '@/utils/themeOverrides';

import type { AppState, PageConfig, PageConfigKeys, Style, Theme } from './types';
import type { RouteRecordRaw } from 'vue-router';

const defaultThemeConfig = {
  style: 'default' as Style,
  customStyle: '#f9fbfb',
  theme: 'default' as Theme,
  customTheme: '#008d91',
};
const defaultLoginConfig = {
  title: 'Cordys CRM',
  icon: [],
  loginLogo: [],
  loginImage: [],
  slogan: 'login.form.title',
};
const defaultPlatformConfig = {
  logoPlatform: [],
  helpDoc: 'https://cordys.cn/docs/',
};

const defaultModuleConfig = [
  {
    moduleKey: ModuleConfigEnum.DASHBOARD,
    enable: true,
  },
  {
    moduleKey: ModuleConfigEnum.AGENT,
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
    versionInfo: {
      currentVersion: '',
      releaseDate: '',
      latestVersion: '',
      architecture: '',
      copyright: '',
      hasNewVersion: false,
    },
    navTopConfigList: [],
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
    getNavTopConfigList: (state: AppState) => {
      const navMap = new Map(defaultNavList.map((n) => [n.key, n]));
      return state.navTopConfigList.map((e) => {
        return { ...navMap.get(e.navigationKey)! };
      });
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
        // TODO license 先放开
        // if (!useLicenseStore().hasLicense()) {
        //   this.moduleConfigList = this.moduleConfigList.filter((e) => e.moduleKey !== ModuleConfigEnum.DASHBOARD);
        // }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    // 初始化顶部导航栏
    async initNavTopConfig() {
      try {
        this.navTopConfigList = await getModuleTopNavList();
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
        this.versionInfo = await getSystemVersion();
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    // 显示 SQLBot
    async showSQLBot() {
      // TODO license 先放开
      // const licenseStore = useLicenseStore();
      // if (!licenseStore.hasLicense()) return;
      const res = await getThirdConfigByType(CompanyTypeEnum.SQLBot);
      await loadScript(res.appSecret as string, { identifier: CompanyTypeEnum.SQLBot });
    },

    // 初始化页面配置
    async initPageConfig() {
      try {
        const res = await getPageConfig();
        if (Array.isArray(res) && res.length > 0) {
          let hasStyleChange = false;
          let hasThemeChange = false;
          // 清空无数据的时候使用默认值
          ['icon', 'loginLogo', 'loginImage', 'logoPlatform'].forEach((key) => {
            this.pageConfig[key as PageConfigKeys] = [
              {
                url: null,
                name: null,
              },
            ] as any;
          });
          res.forEach((e) => {
            const key = e.paramKey.split('ui.')[1] as PageConfigKeys; // 参数名前缀ui.去掉
            if (['icon', 'loginLogo', 'loginImage', 'logoPlatform'].includes(key)) {
              // 四个属性值为文件类型，单独处理
              this.pageConfig[key] = [
                {
                  url: `/ui/display/preview?paramKey=ui.${key}`,
                  name: e.paramValue,
                },
              ] as any;
            } else {
              if (key === 'style') {
                // 风格是否更改，先判断自定义风格的值是否相等，再判断非自定义的俩值是否相等
                hasStyleChange = !['default', 'follow'].includes(e.paramValue)
                  ? this.pageConfig.customStyle !== e.paramValue
                  : this.pageConfig.style !== e.paramValue;
              }
              if (key === 'theme') {
                // 主题是否更改，先判断自定义主题的值是否相等，再判断非自定义的俩值是否相等
                hasThemeChange =
                  e.paramValue !== 'default'
                    ? this.pageConfig.customTheme !== e.paramValue
                    : this.pageConfig.theme !== e.paramValue;
              }
              this.pageConfig[key] = e.paramValue as any;
            }
          });
          if (this.pageConfig.theme !== 'default') {
            // 判断是否选择了自定义主题色
            this.pageConfig.customTheme = this.pageConfig.theme;
            this.pageConfig.theme = 'custom';
          } else {
            // 非自定义则需要重置自定义主题色为空，避免本地缓存与接口配置不一致
            this.pageConfig.customTheme = defaultThemeConfig.customTheme;
          }
          if (!['default', 'follow'].includes(this.pageConfig.style)) {
            // 判断是否选择了自定义平台风格
            this.pageConfig.customStyle = this.pageConfig.style;
            this.pageConfig.style = 'custom';
          } else {
            // 非自定义则需要重置自定义风格，避免本地缓存与接口配置不一致
            this.pageConfig.customStyle = defaultThemeConfig.customStyle;
          }
          // 如果风格和主题有变化，则初始化一下主题和风格；没有变化则不需要在此初始化，在 App.vue 中初始化过了
          if (hasStyleChange) {
            watchStyle(this.pageConfig.style, this.pageConfig);
          }
          if (hasThemeChange) {
            watchTheme(this.pageConfig.theme, this.pageConfig);
          }
          window.document.title = this.pageConfig.title;
        }
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    resetPageConfig() {
      const { t } = useI18n();
      this.pageConfig = { ...this.getDefaultPageConfig, slogan: t(this.defaultLoginConfig.slogan) };
    },
  },
  persist: {
    paths: ['menuIconStatus', 'pageConfig', 'moduleConfigList', 'navTopConfigList'],
  },
});

export default useAppStore;
