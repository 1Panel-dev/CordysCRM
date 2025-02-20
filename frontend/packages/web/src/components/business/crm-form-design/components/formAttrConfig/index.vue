<template>
  <n-tabs v-model:value="configTab" :bar-width="140" justify-content="space-around" type="line" animated>
    <n-tab-pane name="field" :tab="t('crmFormDesign.fieldConfig')">
      <n-scrollbar>
        <fieldAttr :field="fieldConfig" />
      </n-scrollbar>
    </n-tab-pane>
    <n-tab-pane name="form" :tab="t('crmFormDesign.formConfig')">
      <n-scrollbar>
        <formAttr :form-config="formConfig" />
      </n-scrollbar>
    </n-tab-pane>
  </n-tabs>
</template>

<script setup lang="ts">
  import { NScrollbar, NTabPane, NTabs } from 'naive-ui';

  import { FormCreateField } from '@/components/business/crm-form-create/types';
  import fieldAttr from './fieldAttr.vue';
  import formAttr from './formAttr.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { FormConfig } from '../../types';

  const { t } = useI18n();

  const formConfig = defineModel<FormConfig>('formConfig', {
    required: true,
  });

  const fieldConfig = defineModel<FormCreateField>('field', {
    default: null,
  });

  const configTab = ref('field');
</script>

<style lang="less" scoped>
  .n-tabs {
    @apply h-full overflow-hidden;
  }
  :deep(.n-tabs-tab-wrapper) {
    @apply h-full justify-center;

    width: 50%;
    .n-tabs-tab {
      @apply h-full w-full justify-center;

      padding: 8px 0;
    }
  }
  .n-tab-pane {
    @apply h-full;

    --n-pane-padding-top: 0;
  }
</style>
