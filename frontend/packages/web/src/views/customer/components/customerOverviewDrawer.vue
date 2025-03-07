<template>
  <CrmOverviewDrawer
    v-model:show="show"
    v-model:active-tab="activeTab"
    v-model:cached-list="cachedList"
    :tab-list="tabList"
    :button-list="buttonList"
    title="1"
    subtitle="2"
    :form-key="FormDesignKeyEnum.CUSTOMER"
    :source-id="props.sourceId"
  >
    <template #left>
      <CrmFormDescription :form-key="FormDesignKeyEnum.CUSTOMER" :source-id="props.sourceId" />
    </template>
    <template #right>
      <div v-if="activeTab === 'overview'" class="mt-[16px] h-[100px] bg-[var(--text-n10)]"></div>
    </template>
  </CrmOverviewDrawer>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import type { ActionsItem } from '@/components/pure/crm-more-action/type';
  import CrmFormDescription from '@/components/business/crm-form-description/index.vue';
  import CrmOverviewDrawer from '@/components/business/crm-overview-drawer/index.vue';
  import type { TabContentItem } from '@/components/business/crm-tab-setting/type';

  import { useI18n } from '@/hooks/useI18n';

  const props = defineProps<{
    sourceId: string;
  }>();

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
      label: t('overviewDrawer.addContract'),
      key: 'addContract',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('overviewDrawer.followRecord'),
      key: 'followRecord',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('overviewDrawer.followPlan'),
      key: 'followPlan',
      text: false,
      ghost: true,
      class: 'n-btn-outline-primary',
    },
    {
      label: t('common.delete'),
      key: 'delete',
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
