<template>
  <div ref="opportunityCardRef" class="h-full">
    <CrmCard hide-footer no-content-padding>
      <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" />
      <div class="h-[calc(100%-48px)] p-[16px]">
        <CrmOpportunityTable
          :active-tab="activeTab"
          :fullscreen-target-ref="opportunityCardRef"
          @open-customer-drawer="handleOpenCustomerDrawer"
        />
      </div>
    </CrmCard>
  </div>
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
  const opportunityCardRef = ref<HTMLElement | null>(null);

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
