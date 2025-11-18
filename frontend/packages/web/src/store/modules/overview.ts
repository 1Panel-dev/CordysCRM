import { defineStore } from 'pinia';

import useLocalForage from '@/hooks/useLocalForage';
import useUserStore from '@/store/modules/user';

export interface WinOrderConfig {
  priorPeriodEnable: boolean;
  timeField: string;
  userField: string;
  winOrderTimeField: string;
}

export interface OverviewState {
  homeOverviewConfig: Record<string, WinOrderConfig>;
}

const WIN_ORDER_CONFIG_KEY = 'overview_winOrderConfig';

const defaultHomeOverviewConfig: WinOrderConfig = {
  priorPeriodEnable: true,
  timeField: 'CREATE_TIME',
  userField: 'OWNER',
  winOrderTimeField: 'EXPECTED_END_TIME',
};

const useOverviewStore = defineStore('overview', {
  state: (): OverviewState => ({
    homeOverviewConfig: {},
  }),
  actions: {
    //  设置用户首页概览配置
    async setWinOrderConfig(val: WinOrderConfig) {
      const userId = useUserStore().userInfo.id;
      const { setItem } = useLocalForage();

      this.homeOverviewConfig[userId] = val;
      await setItem(WIN_ORDER_CONFIG_KEY, this.homeOverviewConfig);
    },

    // 加载首页概览用户配置
    async loadWinOrderConfig(): Promise<WinOrderConfig> {
      const userId = useUserStore().userInfo.id;
      const { getItem, setItem } = useLocalForage();

      if (this.homeOverviewConfig[userId]) {
        return this.homeOverviewConfig[userId];
      }

      const savedConfig = await getItem<Record<string, WinOrderConfig>>(WIN_ORDER_CONFIG_KEY);

      if (savedConfig && savedConfig[userId]) {
        this.homeOverviewConfig[userId] = savedConfig[userId];
        return savedConfig[userId];
      }

      const userDefaultWinConfig = { ...defaultHomeOverviewConfig };

      this.homeOverviewConfig[userId] = userDefaultWinConfig;
      await setItem(WIN_ORDER_CONFIG_KEY, { ...(savedConfig ?? {}), [userId]: userDefaultWinConfig });

      return userDefaultWinConfig;
    },
  },
});

export default useOverviewStore;
