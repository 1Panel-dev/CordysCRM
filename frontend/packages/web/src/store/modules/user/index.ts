import { defineStore } from 'pinia';

import { useI18n } from '@lib/shared/hooks/useI18n';
import { getGenerateId } from '@lib/shared/method';
import { clearToken, setToken } from '@lib/shared/method/auth';
import { removeRouteListener } from '@lib/shared/method/route-listener';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

import NotifyContent from '@/views/system/message/components/notifyContent.vue';

import { isLogin, login, signout } from '@/api/modules/system/login';
import useDiscreteApi from '@/hooks/useDiscreteApi';
import useUser from '@/hooks/useUser';
import router from '@/router';
import { getFirstRouteNameByPermission } from '@/utils/permission';

import useAppStore from '../app';
import type { NotificationOptions } from 'naive-ui';

const { notification, message } = useDiscreteApi();

export interface UserState {
  loginType: string[];
  userInfo: UserInfo;
  clientIdRandomId: string; // 客户端随机id
}

const useUserStore = defineStore('user', {
  persist: true,
  state: (): UserState => ({
    loginType: [],
    userInfo: {
      id: '',
      name: '',
      email: '',
      password: '',
      enable: true,
      createTime: 0,
      updateTime: 0,
      language: '',
      lastOrganizationId: '',
      phone: '',
      source: '',
      createUser: '',
      updateUser: '',
      platformInfo: '',
      avatar: '',
      permissionIds: [],
      organizationIds: [],
      csrfToken: '',
      sessionId: '',
      roles: [],
      departmentId: '',
      departmentName: '',
    },
    clientIdRandomId: '',
  }),

  getters: {
    isAdmin(state: UserState) {
      return state.userInfo.id === 'admin';
    },
  },
  actions: {
    // 设置用户信息
    setInfo(info: UserInfo) {
      this.$patch({ userInfo: info });
    },
    async login(params: LoginParams) {
      try {
        const res = await login(params);
        setToken(res.sessionId, res.csrfToken);
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds[0] ?? '';
        this.clientIdRandomId = getGenerateId();
        appStore.setOrgId(lastOrganizationId);
      } catch (error) {
        clearToken();
        throw error;
      }
    },
    // 登出回调
    logoutCallBack() {
      const appStore = useAppStore();
      appStore.disconnectSystemMessageSSE();
      // 重置用户信息
      this.$reset();
      clearToken();
      removeRouteListener();
      appStore.hideLoading();
    },
    // 登出
    async logout(silence = false) {
      try {
        const { t } = useI18n();
        if (!silence) {
          const appStore = useAppStore();
          appStore.showLoading(t('message.logouting'));
        }
        await signout();
      } finally {
        this.logoutCallBack();
      }
    },
    // 获取登录认证方式
    async getAuthentication() {
      try {
        // const res = await getAuthenticationList();
        this.loginType = [];
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    qrCodeLogin(res: UserInfo) {
      try {
        if (!res) {
          return false;
        }
        setToken(res.sessionId, res.csrfToken);
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds[0] ?? '';
        appStore.setOrgId(lastOrganizationId);
        this.clientIdRandomId = getGenerateId();
        return true;
      } catch (err) {
        // eslint-disable-next-line no-console
        console.log(err);
        clearToken();
        return false;
      }
    },
    async isLogin() {
      try {
        const res = await isLogin();
        if (!res) {
          return false;
        }
        setToken(res.sessionId, res.csrfToken);
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds[0] ?? '';
        appStore.setOrgId(lastOrganizationId);
        return true;
      } catch (err) {
        // eslint-disable-next-line no-console
        console.log(err);
        return false;
      }
    },
    async checkIsLogin() {
      const { isLoginPage } = useUser();
      const { t } = useI18n();
      const isLoginStatus = await this.isLogin();
      if (isLoginStatus) {
        if (isLoginPage()) {
          const currentRouteName = getFirstRouteNameByPermission(router.getRoutes());
          await router.push({ name: currentRouteName });
        }
      } else if (!isLoginPage()) {
        message.warning(t('message.loginExpired'));
        router.push({ name: 'login' });
      }
    },
    // 展示系统公告
    showSystemNotify() {
      const appStore = useAppStore();
      if (appStore.messageInfo.announcementDTOList?.length) {
        const notify = ref();
        notify.value = notification.create({
          title: '',
          content: () => {
            return h(NotifyContent, {
              onClose: () => notify.value.destroy(),
            });
          },
          duration: undefined,
          maxCount: 1,
        } as NotificationOptions);
      }
    },
  },
});

export default useUserStore;
