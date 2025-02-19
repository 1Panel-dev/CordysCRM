<template>
  <n-scrollbar x-scrollable content-class="flex h-full !w-full bg-[var(--text-n9)]" content-style="min-width: 800px">
    <div class="crm-form-design--left"><fieldComponents @select="handleFieldSelect" /></div>
    <div class="crm-form-design--center">
      <div class="crm-form-design--center-content">
        <formComposition ref="formCompositionRef" v-model:list="list" v-model:field="field" :form-config="formConfig" />
      </div>
    </div>
    <div class="crm-form-design--right"><formAttrConfig :field="field" :form-config="formConfig" /></div>
  </n-scrollbar>
</template>

<script setup lang="ts">
  import { NScrollbar } from 'naive-ui';

  import fieldComponents from './components/fieldComponents.vue';
  import formAttrConfig from './components/formAttrConfig/index.vue';
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
    layout: 1,
    labelPos: 'vertical',
    inputWidth: 'custom',
    optBtnContent: [
      {
        text: t('common.save'),
        enable: true,
      },
      {
        text: t('common.saveAndContinue'),
        enable: false,
      },
      {
        text: t('common.cancel'),
        enable: true,
      },
    ],
    optBtnPos: 'flex-row',
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
