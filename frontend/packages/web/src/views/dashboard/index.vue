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
          <CrmSvgIcon name="dataease" width="14px" height="14px" />
          {{ item.label }}
        </div>
      </CrmTag>
    </div>
    <n-tabs v-model:value="activeDashboardType" type="segment" size="small">
      <n-tab-pane v-if="DEConfig?.deLinkIntegration" name="link" :tab="t('system.business.DE.link')" class="hidden" />
      <n-tab-pane
        v-if="DEConfig?.deModuleEmbedding"
        name="module"
        :tab="t('system.business.DE.embedModule')"
        class="hidden"
      />
    </n-tabs>
    <div class="flex-1">
      <dashboardLink v-if="activeDashboardType === 'link'" />
      <dashboardModule v-else />
    </div>
  </div>
</template>

<script setup lang="ts">
  import { NTabPane, NTabs } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { ConfigSynchronization } from '@lib/shared/models/system/business';

  import CrmSvgIcon from '@/components/pure/crm-svg/index.vue';
  import CrmTag from '@/components/pure/crm-tag/index.vue';
  import dashboardLink from './link.vue';
  import dashboardModule from './module.vue';

  import { getThirdPartyConfig } from '@/api/modules';

  const { t } = useI18n();

  const dashboardList = ref([
    {
      key: 'DE',
      label: 'DataEase',
    },
  ]);
  const activeDashboard = ref(dashboardList.value[0].key);
  const activeDashboardType = ref<'link' | 'module'>('link');

  function clickTag(key: string) {
    activeDashboard.value = key;
  }

  const DEConfig = ref<ConfigSynchronization>();

  async function init() {
    try {
      DEConfig.value = await getThirdPartyConfig('DE_BOARD');
      if (DEConfig.value.deLinkIntegration) {
        activeDashboardType.value = 'link';
      } else {
        activeDashboardType.value = 'module';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  onBeforeMount(() => {
    init();
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
