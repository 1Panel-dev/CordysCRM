<template>
  <div class="process-basic-setting p-[16px]">
    <n-form ref="formRef" class="process-setting-form" :model="form" label-placement="top">
      <n-form-item require-mark-placement="left" path="formType" :label="t('process.process.basic.businessType')">
        <n-select
          v-model:value="form.formType"
          :options="businessTypeOptions"
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
      <n-form-item
        require-mark-placement="right"
        path="name"
        :label="t('process.process.processName')"
        :rule="[{ required: true, message: t('common.notNull', { value: `${t('process.process.processName')}` }) }]"
      >
        <n-input v-model:value="form.name" :maxlength="255" type="text" :placeholder="t('common.pleaseInput')" />
      </n-form-item>
      <n-form-item
        require-mark-placement="right"
        path="executeTiming"
        :label="t('process.process.basic.executionTiming')"
      >
        <n-checkbox-group v-model:value="form.executeTiming" class="mt-[4px]">
          <div class="flex flex-col gap-[8px]">
            <n-checkbox v-for="item of executionTimingList" :key="item.value" :value="item.value">
              <div class="flex items-center gap-[8px]">
                {{ item.label }}
              </div>
            </n-checkbox>
          </div>
        </n-checkbox-group>
      </n-form-item>
      <n-form-item require-mark-placement="right" path="description" :label="t('process.process.basic.description')">
        <n-input
          v-model:value="form.description"
          :maxlength="1000"
          :placeholder="t('common.pleaseInput')"
          type="textarea"
          clearable
        />
      </n-form-item>
    </n-form>
  </div>
</template>

<script setup lang="ts">
  import { NCheckbox, NCheckboxGroup, NForm, NFormItem, NInput, NSelect } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { BasicFormParams } from '@lib/shared/models/system/process';

  import { businessTypeOptions, defaultBasicForm, executionTimingList } from '@/config/process';

  import type { FormInst } from 'naive-ui';

  const { t } = useI18n();
  const form = defineModel<BasicFormParams>('basicConfig', {
    default: () => ({
      ...defaultBasicForm,
    }),
  });

  const formRef = ref<FormInst | null>(null);

  function validate(cb?: () => void) {
    formRef.value?.validate((error) => {
      if (!error) {
        cb?.();
      }
    });
  }

  defineExpose({
    validate,
  });
</script>
