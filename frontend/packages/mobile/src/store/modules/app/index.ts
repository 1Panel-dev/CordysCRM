import { defineStore } from 'pinia';

import { SubscribeMessageUrl } from '@lib/shared/api/requrls/system/message';
import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import { getSSE } from '@lib/shared/method/index';
import { setLocalStorage } from '@lib/shared/method/local-storage';
import { loadScript } from '@lib/shared/method/scriptLoader';

import {
  closeMessageSubscribe,
  getHomeMessageList,
  getKey,
  getModuleNavConfigList,
  getThirdConfigByType,
  getUnReadAnnouncement,
} from '@/api/modules';
import useUserStore from '@/store/modules/user';

import type { AppState } from './types';

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    // 分页
    pageSize: 10,
    showSizePicker: true,
    showQuickJumper: true,
    orgId: '',
    moduleConfigList: [],
    messageInfo: {
      read: true,
      notificationDTOList: [],
      announcementDTOList: [],
    },
    eventSource: null,
    cacheRoutes: new Set([]),
    isManualBack: false,
  }),
  getters: {
    getOrgId(state: AppState) {
      return state.orgId;
    },
    getCacheRoutes(state: AppState) {
      return state.cacheRoutes;
    },
    getManualBack(state: AppState) {
      return state.isManualBack;
    },
  },
  actions: {
    setOrgId(id: string) {
      this.orgId = id;
    },
    addCacheRoute(route: string) {
      this.cacheRoutes.add(route);
    },
    removeCacheRoute(route: string) {
      this.cacheRoutes.delete(route);
    },
    setManualBack(val: boolean) {
      this.isManualBack = val;
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
     * 连接SSE消息订阅流
     */
    async connectSystemMessageSSE() {
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
     * 初始化首页消息
     */
    async initMessage() {
      try {
        const [notifications, announcements] = await Promise.all([getHomeMessageList(), getUnReadAnnouncement()]);
        this.messageInfo.notificationDTOList = notifications;
        this.messageInfo.announcementDTOList = announcements;
        this.messageInfo.read = !(announcements?.length || notifications?.length);
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
    async initPublicKey() {
      try {
        const res = await getKey();
        setLocalStorage('publicKey', res);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.error(error);
      }
    },
  },
  persist: {
    paths: [''],
  },
});

export default useAppStore;
