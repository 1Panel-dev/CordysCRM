<template>
  <n-tabs v-model:value="configTab" :bar-width="140" justify-content="space-around" animated>
    <n-tab-pane name="field" :tab="t('crmFormDesign.fieldConfig')">
      <div class="p-[16px]">
        <div v-if="props.field"> field </div>
        <div v-else class="flex justify-center py-[44px] text-[var(--text-n4)]">
          {{ t('crmFormDesign.fieldConfigEmptyTip') }}
        </div>
      </div>
    </n-tab-pane>
    <n-tab-pane name="form" :tab="t('crmFormDesign.formConfig')"> form </n-tab-pane>
  </n-tabs>
</template>

<script setup lang="ts">
  import { NTabPane, NTabs } from 'naive-ui';

  import { FormCreateField } from '@/components/business/crm-form-create/types';

  import { useI18n } from '@/hooks/useI18n';

  import { FormConfig } from '../types';

  const props = defineProps<{
    field?: FormCreateField;
    formConfig: FormConfig;
  }>();

  const { t } = useI18n();

  const configTab = ref('field');
</script>

<style lang="less" scoped>
  :deep(.n-tabs-tab-wrapper) {
    @apply justify-center;

    width: 50%;
    .n-tabs-tab {
      @apply w-full justify-center;

      padding: 8px 0;
    }
  }
</style>
