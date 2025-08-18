import { defineStore } from 'pinia';
import { showToast } from 'vant';

import { CompanyTypeEnum } from '@lib/shared/enums/commonEnum';
import { useI18n } from '@lib/shared/hooks/useI18n';
import { getGenerateId } from '@lib/shared/method';
import { clearToken, setToken } from '@lib/shared/method/auth';
import { removeRouteListener } from '@lib/shared/method/route-listener';
import { removeScript } from '@lib/shared/method/scriptLoader';
import type { LoginParams } from '@lib/shared/models/system/login';
import type { UserInfo } from '@lib/shared/models/user';

import { isLogin, login, signout } from '@/api/modules';
import useUser from '@/hooks/useUser';
import router from '@/router';

import { AppRouteEnum } from '@/enums/routeEnum';

import useAppStore from '../app';

export interface UserState {
  loginType: string[];
  userInfo: UserInfo;
  clientIdRandomId: string;
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
    setLoginInfo(res: UserInfo) {
      try {
        if (!res) {
          return false;
        }
        setToken(res.sessionId, res.csrfToken);
        this.setInfo(res);
        const appStore = useAppStore();
        const lastOrganizationId = res.lastOrganizationId ?? res.organizationIds?.[0] ?? '';
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
        this.setLoginInfo(res);
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
      if (isLoginStatus) {
        if (isLoginPage()) {
          router.replace({ name: AppRouteEnum.WORKBENCH });
        }
      } else if (!isLoginPage()) {
        router.replace({ name: 'login' });
      }
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
    logoutCallBack() {
      this.$reset();
      clearToken();
      removeRouteListener();
      removeScript(CompanyTypeEnum.SQLBot);
    },
    async logout(silence = false) {
      try {
        const { t } = useI18n();
        if (!silence) {
          showToast(t('message.logouting'));
        }
        await signout();
      } finally {
        this.logoutCallBack();
      }
    },
  },
});

export default useUserStore;
