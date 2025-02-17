import { defineStore } from 'pinia';

import { isLogin, login, signout } from '@/api/modules/system/login';
import { useI18n } from '@/hooks/useI18n';

import useAppStore from '../app';
import { clearToken, setToken } from '@lib/shared/method/auth';
import { removeRouteListener } from '@lib/shared/method/route-listener';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

export interface UserState {
  loginType: string[];
  userInfo: UserInfo;
}

const useUserStore = defineStore('user', {
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
    },
  }),

  getters: {},
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
        appStore.setOrgId(lastOrganizationId);
      } catch (error) {
        clearToken();
        throw error;
      }
    },
    // 登出回调
    logoutCallBack() {
      const appStore = useAppStore();
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
    qrCodeLogin(params: any) {},
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
        return false;
      }
    },
  },
});

export default useUserStore;
