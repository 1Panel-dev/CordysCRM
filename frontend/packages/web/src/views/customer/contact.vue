<template>
  <ContactTable ref="contactTableRef" :form-key="FormDesignKeyEnum.CONTACT" />
  <customerOverviewDrawer
    v-if="isInitCustomerDrawer"
    v-model:show="showCustomerOverviewDrawer"
    :source-id="activeSourceId"
    @saved="searchData()"
  />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';

  const customerOverviewDrawer = defineAsyncComponent(
    () => import('@/views/customer/components/customerOverviewDrawer.vue')
  );

  const route = useRoute();

  const contactTableRef = ref<InstanceType<typeof ContactTable>>();

  const showCustomerOverviewDrawer = ref(false);
  const activeSourceId = ref<string>('');
  const isInitCustomerDrawer = ref(false);

  function searchData() {
    contactTableRef.value?.handleSearchData?.();
  }

  onMounted(() => {
    if (route.query.id) {
      isInitCustomerDrawer.value = true;
      activeSourceId.value = route.query.id as string;
      showCustomerOverviewDrawer.value = true;
    }
  });
</script>
