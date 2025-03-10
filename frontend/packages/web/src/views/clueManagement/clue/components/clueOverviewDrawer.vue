<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    title="111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111"
    subtitle="啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊"
    :form-key="FormDesignKeyEnum.CLUE"
  >
    <template #right>
      <div v-if="activeTab === 'overview'" class="h-[1000px] bg-[var(--text-n10)]"></div>
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const show = defineModel<boolean>('show', {
    required: true,
  });

  const buttonList: ActionsItem[] = [
    {
      label: t('common.edit'),
      key: 'edit',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('org.resetPassWord'),
      key: 'resetPassWord',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
  ];

  const activeTab = ref('overview');
  const cachedList = ref([]);
  const tabList: TabContentItem[] = [
    {
      name: 'overview',
      tab: t('common.overview'),
      enable: true,
      allowClose: false,
    },
    {
      name: 'followRecord',
      tab: t('crmFollowRecord.followRecord'),
      enable: true,
      allowClose: true,
    },
    {
      name: 'followPlan',
      tab: t('common.followPlan'),
      enable: true,
      allowClose: true,
    },
    {
      name: 'headRecord',
      tab: t('common.headRecord'),
      enable: true,
      allowClose: true,
    },
  ];
</script>
