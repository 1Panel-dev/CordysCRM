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
  <openSeaOverviewDrawer
    v-model:show="showCustomerOpenseaOverviewDrawer"
    :source-id="activeSourceId"
    :readonly="isCustomerReadonly"
    :pool-id="poolId"
  />
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmOpportunityTable from './components/opportunityTable.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  const opportunityCardRef = ref<HTMLElement | null>(null);

  const showCustomerOverviewDrawer = ref(false);
  const showCustomerOpenseaOverviewDrawer = ref(false);
  const poolId = ref<string>('');
  const activeSourceId = ref<string>('');
  const isCustomerReadonly = ref(false);
  function handleOpenCustomerDrawer(
    params: { customerId: string; inCustomerPool: boolean; poolId: string },
    readonly = false
  ) {
    activeSourceId.value = params.customerId;
    if (params.inCustomerPool) {
      showCustomerOpenseaOverviewDrawer.value = true;
      poolId.value = params.poolId;
    } else {
      showCustomerOverviewDrawer.value = true;
    }
    isCustomerReadonly.value = readonly;
  }
</script>

<style scoped></style>
