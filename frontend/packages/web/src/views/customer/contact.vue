<template>
  <ContactTable ref="contactTableRef" :form-key="FormDesignKeyEnum.CONTACT" />
  <customerOverviewDrawer
    v-if="isInitCustomerDrawer"
    v-model:show="showCustomerOverviewDrawer"
    :source-id="activeSourceId"
    @saved="searchData()"
  />
  <openSeaOverviewDrawer
    v-model:show="showOpenSeaOverviewDrawer"
    :pool-id="poolId"
    :source-id="activeSourceId"
    :hidden-columns="hiddenColumns"
    @change="searchData"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { CluePoolItem } from '@lib/shared/models/system/module';

  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';
  import openSeaOverviewDrawer from '@/views/customer/components/openSeaOverviewDrawer.vue';

  import { getOpenSeaOptions } from '@/api/modules';
  import { hasAnyPermission } from '@/utils/permission';

  const customerOverviewDrawer = defineAsyncComponent(
    () => import('@/views/customer/components/customerOverviewDrawer.vue')
  );

  const route = useRoute();

  const contactTableRef = ref<InstanceType<typeof ContactTable>>();

  const showOpenSeaOverviewDrawer = ref<boolean>(false);
  const poolId = ref<string>('');

  const showCustomerOverviewDrawer = ref(false);
  const activeSourceId = ref<string>('');
  const isInitCustomerDrawer = ref(false);

  function searchData() {
    contactTableRef.value?.handleSearchData?.();
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

  onMounted(async () => {
    await initOpenSeaOptions();
    if (!route.query.id) return;
    activeSourceId.value = route.query.id as string;
    if (route.query.inSharedPool === 'true') {
      poolId.value = (route.query.poolId as string) ?? '';
      showOpenSeaOverviewDrawer.value = true;
    } else {
      isInitCustomerDrawer.value = true;
      showCustomerOverviewDrawer.value = true;
    }
  });
</script>
