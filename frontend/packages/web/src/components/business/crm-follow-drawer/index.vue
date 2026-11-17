<template>
  <CrmDrawer
    v-model:show="visible"
    resizable
    no-padding
    :width="1200"
    :footer="false"
    class="min-w-[1200px]"
    :title="t('settings.navbar.event')"
  >
    <div class="flex h-full flex-col bg-[var(--text-n9)] px-[16px] pt-[16px]">
      <CrmCard v-if="tabList.length" no-content-padding hide-footer auto-height class="mb-[16px]">
        <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
      </CrmCard>
      <div v-if="visible && tabList.length" class="flex-1">
        <Suspense>
          <template v-if="activeTab === 'followRecord'">
            <RecordTable />
          </template>
          <template v-else-if="activeTab === 'followPlan'">
            <PlanTable />
          </template>
        </Suspense>
      </div>
    </div>
  </CrmDrawer>
</template>

<script lang="ts" setup>
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmDrawer from '@/components/pure/crm-drawer/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';

  import { hasAnyPermission } from '@/utils/permission';

  const RecordTable = defineAsyncComponent(() => import('./components/recordTable.vue'));
  const PlanTable = defineAsyncComponent(() => import('./components/planTable.vue'));

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const { t } = useI18n();

  const activeTab = ref('followRecord');

  const tabList = computed(() =>
    [
      {
        name: 'followRecord',
        tab: t('crmFollowRecord.followRecord'),
        permission: ['FOLLOW_RECORD:READ'],
      },
      {
        name: 'followPlan',
        tab: t('common.plan'),
        permission: ['FOLLOW_PLAN:READ'],
      },
    ].filter((item) => hasAnyPermission(item.permission))
  );

  watch(
    tabList,
    (tabs) => {
      if (!tabs.some((tab) => tab.name === activeTab.value)) {
        activeTab.value = tabs[0]?.name || '';
      }
    },
    { immediate: true }
  );
</script>
