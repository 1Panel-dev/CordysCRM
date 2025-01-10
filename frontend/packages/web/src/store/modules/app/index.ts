import { defineStore } from 'pinia';

export interface AppState {
  pageSize: number;
  showSizePicker: boolean;
  showQuickJumper: boolean;
  menuCollapsed: boolean; // 侧边菜单栏是否收缩
  loginLoading: boolean; // 登录页面加载中
}

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    menuCollapsed: false,
    // 分页
    pageSize: 10,
    showSizePicker: true,
    showQuickJumper: true,
    loginLoading: false,
  }),
  getters: {
    getMenuCollapsed(state: AppState) {
      return state.menuCollapsed;
    },
    getLoginLoadingStatus(state: AppState): boolean {
      return state.loginLoading;
    },
  },
  actions: {
    setMenuCollapsed(collapsed: boolean) {
      this.menuCollapsed = collapsed;
    },
    setLoginLoading(value: boolean) {
      this.loginLoading = value;
    },
  },
  persist: {
    paths: [''],
  },
});

export default useAppStore;
