<template>
  <CrmFormDrawer v-model:visible="visible" :title="title" :form-key="FormDesignKeyEnum.CUSTOMER"> </CrmFormDrawer>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';

  import CrmFormDrawer from '@/components/business/crm-form-design-drawer/index.vue';

  import { useI18n } from '@/hooks/useI18n';

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });

  const title = ref(t('customManagement.newCustomer'));
</script>

<style lang="less" scoped></style>
