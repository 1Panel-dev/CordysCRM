<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
  </CrmCard>
  <CrmCard hide-footer :special-height="64">
    <CrmOpportunityTable :active-tab="activeTab" @open-customer-drawer="handleOpenCustomerDrawer" />
  </CrmCard>
  <customerOverviewDrawer v-model:show="showCustomerOverviewDrawer" :source-id="activeSourceId" />
</template>

<script setup lang="ts">
  import { TabPaneProps } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { OpportunitySearchTypeEnum } from '@lib/shared/enums/opportunityEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import CrmOpportunityTable from './components/opportunityTable.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';

  import useHiddenTab from '@/hooks/useHiddenTab';

  const { t } = useI18n();

  const allTabList: TabPaneProps[] = [
    {
      name: OpportunitySearchTypeEnum.ALL,
      tab: t('opportunity.allOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.SELF,
      tab: t('opportunity.myOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.DEPARTMENT,
      tab: t('opportunity.departmentOpportunities'),
    },
    {
      name: OpportunitySearchTypeEnum.DEAL,
      tab: t('opportunity.convertedOpportunities'),
    },
  ];

  const { tabList, activeTab } = useHiddenTab(allTabList, FormDesignKeyEnum.BUSINESS);

  const showCustomerOverviewDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenCustomerDrawer(sourceId: string) {
    activeSourceId.value = sourceId;
    showCustomerOverviewDrawer.value = true;
  }
</script>

<style scoped></style>
