import { defineStore } from 'pinia';

export interface UserState {
  loginType: string[];
}

const useAppStore = defineStore('user', {
  state: (): UserState => ({
    loginType: [],
  }),

  getters: {},
  actions: {
    login(params: Record<string, any>) {
      // do something
    },
    logout() {
      // do something
    },
    // 获取登录认证方式
    async getAuthentication() {
      try {
        // const res = await getAuthenticationList();
        this.loginType = ['LDAP', 'OIDC', 'OAUTH2', 'CAS'];
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      }
    },
    qrCodeLogin(params: any) {},
  },
});

export default useAppStore;
