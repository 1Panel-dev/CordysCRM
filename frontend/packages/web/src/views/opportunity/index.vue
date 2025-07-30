<template>
  <div ref="opportunityCardRef" class="h-full">
    <CrmCard hide-footer no-content-padding>
      <div class="h-full p-[16px] !pb-0">
        <CrmOpportunityTable
          :fullscreen-target-ref="opportunityCardRef"
          @open-customer-drawer="handleOpenCustomerDrawer"
        />
      </div>
    </CrmCard>
  </div>
  <customerOverviewDrawer
    v-model:show="showCustomerOverviewDrawer"
    :source-id="activeSourceId"
    :readonly="isCustomerReadonly"
  />
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmOpportunityTable from './components/opportunityTable.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';

  const opportunityCardRef = ref<HTMLElement | null>(null);

  const showCustomerOverviewDrawer = ref(false);
  const activeSourceId = ref<string>('');
  const isCustomerReadonly = ref(false);
  function handleOpenCustomerDrawer(sourceId: string, readonly: boolean) {
    activeSourceId.value = sourceId;
    showCustomerOverviewDrawer.value = true;
    isCustomerReadonly.value = readonly;
  }
</script>

<style scoped></style>
