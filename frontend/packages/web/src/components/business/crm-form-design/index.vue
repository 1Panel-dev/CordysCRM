<template>
  <n-scrollbar x-scrollable content-class="flex h-full !w-full bg-[var(--text-n9)]" content-style="min-width: 800px">
    <div class="crm-form-design--left"><fieldComponents @select="handleFieldSelect" /></div>
    <div class="crm-form-design--center">
      <div class="crm-form-design--center-content">
        <formComposition ref="formCompositionRef" v-model:list="list" v-model:field="field" :form-config="formConfig" />
      </div>
    </div>
    <div class="crm-form-design--right"><attrConfig :field="field" :form-config="formConfig" /></div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import attrConfig from './components/attrConfig.vue';
  import fieldComponents from './components/fieldComponents.vue';
  import formComposition from './components/formComposition.vue';

  import { useI18n } from '@/hooks/useI18n';

  import { FormCreateField } from '../crm-form-create/types';
  import { FormConfig } from './types';

  const { t } = useI18n();

  const list = ref<FormCreateField[]>([]);
  const field = ref<FormCreateField>();
  const formCompositionRef = ref<InstanceType<typeof formComposition>>();

  function handleFieldSelect(item: FormCreateField) {
    formCompositionRef.value?.addItem(item);
  }

  const formConfig = ref<FormConfig>({
    formCols: 4,
    okText: t('common.save'),
    continueText: t('common.saveAndContinue'),
    cancelText: t('common.cancel'),
    footerDirectionClass: 'flex-row',
  });
</script>

<style lang="less" scoped>
  .crm-form-design--left,
  .crm-form-design--right {
    @apply h-full;

    width: 280px;
    background-color: var(--text-n10);
  }
  .crm-form-design--left {
    padding: 16px;
  }
  .crm-form-design--center {
    @apply h-full flex-1 bg-transparent;

    padding: 16px;
    .crm-form-design--center-content {
      @apply h-full;

      border-radius: var(--border-radius-small);
      background-color: var(--text-n10);
    }
  }
</style>
