import { defineStore } from 'pinia';

import { getGenerateId } from '@lib/shared/method';
import { clearToken, setToken } from '@lib/shared/method/auth';
import type { UserInfo } from '@lib/shared/models/user';

import { isLogin } from '@/api/modules';

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
      const isLoginStatus = await this.isLogin();
      return isLoginStatus;
    },
  },
});

export default useUserStore;
