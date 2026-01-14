<template>
  <div ref="invoiceCardRef" class="h-full">
    <CrmCard no-content-padding hide-footer>
      <div class="h-full px-[16px] pt-[16px]">
        <InvoiceTable :fullscreen-target-ref="invoiceCardRef" @open-contract-drawer="handleOpenContractDrawer" />
      </div>
    </CrmCard>
    <ContractDetailDrawer
      v-model:visible="showContractDetailDrawer"
      :sourceId="activeSourceId"
      @showCustomerDrawer="handleOpenCustomerDrawer"
    />
    <customerOverviewDrawer v-model:show="showCustomerOverviewDrawer" :source-id="activeCustomerSourceId" />
    <openSeaOverviewDrawer
      v-model:show="showCustomerOpenseaOverviewDrawer"
      :source-id="activeCustomerSourceId"
      :pool-id="poolId"
      :hidden-columns="hiddenColumns"
    />
  </div>
</template>

<script setup lang="ts">
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import ContractDetailDrawer from '../contract/components/detail.vue';
  import InvoiceTable from './components/invoiceTable.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { getOpenSeaOptions } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const invoiceCardRef = ref<HTMLElement | null>(null);

  const showContractDetailDrawer = ref(false);
  const activeSourceId = ref<string>('');
  function handleOpenContractDrawer(params: { id: string }) {
    activeSourceId.value = params.id;
    showContractDetailDrawer.value = true;
  }

  const showCustomerOverviewDrawer = ref(false);
  const showCustomerOpenseaOverviewDrawer = ref(false);
  const poolId = ref<string>('');
  const activeCustomerSourceId = ref<string>('');
  function handleOpenCustomerDrawer(params: { customerId: string; inCustomerPool: boolean; poolId: string }) {
    activeCustomerSourceId.value = params.customerId;
    if (params.inCustomerPool) {
      showCustomerOpenseaOverviewDrawer.value = true;
      poolId.value = params.poolId;
    } else {
      showCustomerOverviewDrawer.value = true;
    }
  }

  const openSeaOptions = ref<CluePoolItem[]>([]);

  async function initOpenSeaOptions() {
    if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
      const res = await getOpenSeaOptions();
      openSeaOptions.value = res;
    }
  }

  const hiddenColumns = computed<string[]>(() => {
    const openSeaSetting = openSeaOptions.value.find((item) => item.id === poolId.value);
    return openSeaSetting?.fieldConfigs.filter((item) => !item.enable).map((item) => item.fieldId) || [];
  });

  onBeforeMount(() => {
    initOpenSeaOptions();
  });
</script>
