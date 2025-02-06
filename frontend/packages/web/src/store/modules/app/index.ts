import { defineStore } from 'pinia';

import { getKey } from '@/api/modules/system/login';
import { useI18n } from '@/hooks/useI18n';

import { setLocalStorage } from '@lib/shared/method/local-storage';

export interface AppState {
  pageSize: number;
  showSizePicker: boolean;
  showQuickJumper: boolean;
  menuCollapsed: boolean; // 侧边菜单栏是否收缩
  collapsedWidth: number; // 侧边菜单栏收缩宽度
  loginLoading: boolean; // 登录页面加载中
  loading: boolean; // 全局加载中
  loadingTip: string; // 全局加载提示
}

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    menuCollapsed: false,
    collapsedWidth: 56,
    // 分页
    pageSize: 10,
    showSizePicker: true,
    showQuickJumper: true,
    loginLoading: false,
    loading: false,
    loadingTip: '',
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
    /**
     * 显示全局 loading
     */
    showLoading(tip = '') {
      const { t } = useI18n();
      this.loading = true;
      this.loadingTip = tip || t('message.loadingDefaultTip');
    },
    /**
     * 隐藏全局 loading
     */
    hideLoading() {
      const { t } = useI18n();
      this.loading = false;
      this.loadingTip = t('message.loadingDefaultTip');
    },
    setLoginLoading(value: boolean) {
      this.loginLoading = value;
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
