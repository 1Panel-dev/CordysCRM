import { defineStore } from 'pinia';

export interface AppState {
  menuCollapsed: boolean;
}

const useAppStore = defineStore('app', {
  state: (): AppState => ({
    menuCollapsed: false,
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
});

export default useAppStore;
