import { defineStore } from 'pinia';

import { isLogin, login, signout } from '@/api/modules/system/login';
import { useI18n } from '@/hooks/useI18n';

import useAppStore from '../app';
import { clearToken } from '@lib/shared/method/auth';
import { removeRouteListener } from '@lib/shared/method/route-listener';
import type { LoginParams } from '@lib/shared/models/system/login';

export interface UserState {
  loginType: string[];
  orgId: string;
}

const useUserStore = defineStore('user', {
  state: (): UserState => ({
    loginType: [],
    orgId: '',
  }),

  getters: {
    getOrgId(state: UserState) {
      return state.orgId;
    },
  },
  actions: {
    async login(params: LoginParams) {
      try {
        await login(params);
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
        throw new Error(error as string);
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
        await isLogin();
        return true;
      } catch (err) {
        return false;
      }
    },
  },
});

export default useUserStore;
