<template>
  <div class="flex h-full flex-col gap-[8px]">
    <div class="flex w-full items-center gap-[12px] bg-[var(--text-n10)] p-[8px]">
      <CrmTag
        v-for="item of dashboardList"
        :key="item.key"
        :type="activeDashboard === item.key ? 'primary' : 'default'"
        theme="outline"
        class="cursor-pointer !px-[12px]"
        size="large"
        tooltip-disabled
        @click="clickTag(item.key)"
      >
        <div class="flex items-center gap-[8px]">
          <CrmSvgIcon v-if="item.key === 'DE'" :name="item.icon" width="14px" height="14px" />
          <CrmIcon v-else :type="item.icon" />
          {{ item.label }}
        </div>
      </CrmTag>
    </div>
    <div class="flex-1">
      <dashboardLink v-if="activeDashboard === 'LINK'" />
      <dashboardModule v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ConfigSynchronization } from '@lib/shared/models/system/business';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';
  import CrmSvgIcon from '@/components/pure/crm-svg/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import dashboardLink from './link.vue';
  import dashboardModule from './module.vue';

  import { getThirdPartyConfig } from '@/api/modules';
  import useLocalForage from '@/hooks/useLocalForage';

  const { t } = useI18n();
  const { setItem, getItem } = useLocalForage();
  const fullList = [
    {
      key: 'LINK',
      label: t('system.business.DE.link'),
      icon: 'iconicon_link2',
    },
    {
      key: 'DE',
      label: 'DataEase',
      icon: 'dataease',
    },
  ];

  const DEConfig = ref<ConfigSynchronization>();

  async function init() {
    try {
      DEConfig.value = await getThirdPartyConfig('DE_BOARD');
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  const dashboardList = computed(() => {
    return fullList.filter((item) => (item.key === 'DE' && DEConfig.value?.deBoardEnable) || item.key === 'LINK');
  });
  const activeDashboard = ref(dashboardList.value[0].key);
  const activeDashboardKey = 'dashboard-active-tab';
  async function clickTag(key: string) {
    activeDashboard.value = key;
    await setItem(activeDashboardKey, key);
  }

  async function loadActiveDashboard() {
    const currentActiveKey = await getItem(activeDashboardKey);
    if (typeof currentActiveKey === 'string' && dashboardList.value.some((item) => item.key === currentActiveKey)) {
      activeDashboard.value = currentActiveKey;
    } else {
      activeDashboard.value = dashboardList.value[0]?.key ?? '';
      await setItem(activeDashboardKey, activeDashboard.value);
    }
  }

  onBeforeMount(async () => {
    await init();
    loadActiveDashboard();
  });
</script>

<style lang="less" scoped>
  .dashboard-card {
    padding: 24px;
    border: 1px solid var(--text-n8);
    border-radius: 6px;
    .dashboard-card-header {
      @apply flex items-start justify-between;

      margin-bottom: 8px;
      padding-bottom: 16px;
    }
    .dashboard-card-bottom {
      @apply flex items-center;

      gap: 12px;
    }
  }
</style>
