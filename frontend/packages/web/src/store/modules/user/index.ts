import { defineStore } from 'pinia';

import { clearToken, setToken } from '@lib/shared/method/auth';
import { removeRouteListener } from '@lib/shared/method/route-listener';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

import { isLogin, login, signout } from '@/api/modules/system/login';
import { useI18n } from '@/hooks/useI18n';
import useUser from '@/hooks/useUser';
import router from '@/router';

import { AppRouteEnum } from '@/enums/routeEnum';

import useAppStore from '../app';

export interface UserState {
  loginType: string[];
  userInfo: UserInfo;
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
    },
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
      const isLoginStatus = await this.isLogin();
      if (isLoginPage() && isLoginStatus) {
        // 当前页面为登录页面，且已经登录，跳转到首页
        // TODO lmy 跳转到有权限的第一个路由名
        await router.push({ name: AppRouteEnum.SYSTEM });
      }
    },
  },
});

export default useUserStore;
