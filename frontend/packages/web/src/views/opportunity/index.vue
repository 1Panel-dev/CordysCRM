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
  <customerOverviewDrawer v-model:show="showCustomerOverviewDrawer" :source-id="activeSourceId" />
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmOpportunityTable from './components/opportunityTable.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';

  const opportunityCardRef = ref<HTMLElement | null>(null);

  const showCustomerOverviewDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenCustomerDrawer(sourceId: string) {
    activeSourceId.value = sourceId;
    showCustomerOverviewDrawer.value = true;
  }
</script>

<style scoped></style>
