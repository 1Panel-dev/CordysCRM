<template>
  <CrmCard no-content-padding hide-footer auto-height class="mb-[16px]">
    <CrmTab v-model:active-tab="activeTab" no-content :tab-list="tabList" type="line" @change="handleTabChange" />
  </CrmCard>
  <ContactTable
    ref="contactTableRef"
    :special-height="64"
    :form-key="FormDesignKeyEnum.CONTACT"
    :search-type="activeTab"
  />
</template>

<script setup lang="ts">
  import { TabPaneProps } from 'naive-ui';

  import { CustomerSearchTypeEnum } from '@lib/shared/enums/customerEnum';
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmCard from '@/components/pure/crm-card/index.vue';
  import CrmTab from '@/components/pure/crm-tab/index.vue';
  import ContactTable from '@/components/business/crm-form-create-table/contactTable.vue';

  import useHiddenTab from '@/hooks/useHiddenTab';

  const { t } = useI18n();

  const allTabList: TabPaneProps[] = [
    {
      name: CustomerSearchTypeEnum.ALL,
      tab: t('customer.contacts.all'),
    },
    {
      name: CustomerSearchTypeEnum.SELF,
      tab: t('customer.contacts.mine'),
    },
    {
      name: CustomerSearchTypeEnum.DEPARTMENT,
      tab: t('customer.contacts.department'),
    },
  ];
  const { tabList, activeTab } = useHiddenTab(allTabList, FormDesignKeyEnum.CONTACT);

  const contactTableRef = ref<InstanceType<typeof ContactTable>>();
  function handleTabChange() {
    nextTick(() => {
      contactTableRef.value?.searchData();
    });
  }
</script>
