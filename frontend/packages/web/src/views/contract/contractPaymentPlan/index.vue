<template>
  <div ref="contractPaymentCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <PaymentTable
          :fullscreen-target-ref="contractPaymentCardRef"
          @open-contract-drawer="handleOpenContractDrawer"
        />
      </div>
    </CrmCard>
    <ContractDetailDrawer v-model:visible="showContractDetailDrawer" :sourceId="activeSourceId" />
  </div>
</template>

<script setup lang="ts">
  import CrmCard from '@/components/pure/crm-card/index.vue';
  import ContractDetailDrawer from '../contract/components/detail.vue';
  import PaymentTable from './components/paymentTable.vue';

  const contractPaymentCardRef = ref<HTMLElement | null>(null);

  const showContractDetailDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenContractDrawer(params: { id: string }) {
    activeSourceId.value = params.id;
    showContractDetailDrawer.value = true;
  }
</script>
