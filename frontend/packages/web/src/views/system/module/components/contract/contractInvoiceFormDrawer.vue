<template>
  <CrmFormDrawer
    v-model:visible="visible"
    :title="t('module.contract.invoiceFormSetting')"
    :form-key="FormDesignKeyEnum.INVOICE"
  >
  </CrmFormDrawer>
</template>

<script setup lang="ts">
  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import CrmFormDrawer from '@/components/business/crm-form-design-drawer/index.vue';

  const { t } = useI18n();

  const visible = defineModel<boolean>('visible', {
    required: true,
  });
</script>

<style lang="less" scoped></style>
