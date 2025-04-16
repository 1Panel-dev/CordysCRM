import { defineStore } from 'pinia';

import { SubscribeMessageUrl } from '@lib/shared/api/requrls/system/message';
import { getSSE } from '@lib/shared/method/index';

import { closeMessageSubscribe, getHomeMessageList, getUnReadAnnouncement } from '@/api/modules';
import useUserStore from '@/store/modules/user';
import { hasAnyPermission } from '@/utils/permission';

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
  }),
  getters: {
    getOrgId(state: AppState) {
      return state.orgId;
    },
  },
  actions: {
    setOrgId(id: string) {
      this.orgId = id;
    },
    /**
     * 初始化模块配置
     */
    // async initModuleConfig() {
    //   try {
    //     this.moduleConfigList = await getModuleNavConfigList({ organizationId: this.orgId });
    //   } catch (error) {
    //     // eslint-disable-next-line no-console
    //     console.log(error);
    //   }
    // },

    /**
     * 连接SSE消息订阅流
     */
    async connectSystemMessageSSE() {
      if (!hasAnyPermission(['SYSTEM_NOTICE:READ'])) {
        return;
      }
      const userStore = useUserStore();

      await this.disconnectSystemMessageSSE();
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
      if (!hasAnyPermission(['SYSTEM_NOTICE:READ'])) {
        return;
      }
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
  },
  persist: {
    paths: [''],
  },
});

export default useAppStore;
