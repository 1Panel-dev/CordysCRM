import { defineStore } from 'pinia';

import type { AppState } from './types';

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    // 分页
    pageSize: 10,
    showSizePicker: true,
    showQuickJumper: true,
    orgId: '',
    moduleConfigList: [],
  }),
  getters: {
    getOrgId(state: AppState) {
      return state.orgId;
    },
  },
  actions: {
    setOrgId(id: string) {
      this.orgId = id;
    },
    /**
     * 初始化模块配置
     */
    // async initModuleConfig() {
    //   try {
    //     this.moduleConfigList = await getModuleNavConfigList({ organizationId: this.orgId });
    //   } catch (error) {
    //     // eslint-disable-next-line no-console
    //     console.log(error);
    //   }
    // },
  },
  persist: {
    paths: [''],
  },
});

export default useAppStore;
