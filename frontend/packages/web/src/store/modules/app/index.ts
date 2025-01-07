import { defineStore } from 'pinia';

import type { PaginationSizeOption } from 'naive-ui';

export interface AppState {
  currentProjectId: string;
  menuCollapsed: boolean;
  pageSize: number;
  showSizePicker: boolean;
  showQuickJumper: boolean;
  pageSizes: Array<number | PaginationSizeOption>;
}

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    currentProjectId: '',
    menuCollapsed: false,
    // 分页
    pageSize: 10,
    showSizePicker: true,
    showQuickJumper: true,
    pageSizes: [10, 20, 30, 40, 50],
  }),
  getters: {
    getMenuCollapsed(state: AppState) {
      return state.menuCollapsed;
    },
  },
  actions: {
    setMenuCollapsed(collapsed: boolean) {
      this.menuCollapsed = collapsed;
    },
  },
  persist: {
    paths: ['currentProjectId'],
  },
});

export default useAppStore;
