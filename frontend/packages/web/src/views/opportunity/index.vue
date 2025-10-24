<template>
  <div ref="opportunityCardRef" class="h-full">
    <CrmCard hide-footer no-content-padding>
      <div class="h-full p-[16px]">
        <CrmOpportunityTable
          :fullscreen-target-ref="opportunityCardRef"
          :opensea-hidden-columns="hiddenColumns"
          :form-key="FormDesignKeyEnum.BUSINESS"
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
    :hidden-columns="hiddenColumns"
  />
</template>

<script setup lang="ts">
  import { useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmOpportunityTable from './components/opportunityTable.vue';
  import customerOverviewDrawer from '@/views/customer/components/customerOverviewDrawer.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { getOpenSeaOptions } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const { t } = useI18n();
  const Message = useMessage();
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
      if (hasAnyPermission(['CUSTOMER_MANAGEMENT_POOL:READ'])) {
        showCustomerOpenseaOverviewDrawer.value = true;
        poolId.value = params.poolId;
      } else {
        Message.warning(t('opportunity.noOpenSeaPermission'));
      }
    } else {
      showCustomerOverviewDrawer.value = true;
    }
    isCustomerReadonly.value = readonly;
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

<style scoped></style>
