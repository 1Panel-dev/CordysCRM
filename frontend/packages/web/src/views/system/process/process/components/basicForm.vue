<template>
  <div class="process-basic-setting p-[16px]">
    <n-form ref="formRef" class="process-basic-setting-form" :model="form" label-placement="top">
      <n-form-item require-mark-placement="left" path="businessType" :label="t('process.process.basic.businessType')">
        <n-select
          v-model:value="form.businessType"
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
        path="executionTiming"
        :label="t('process.process.basic.executionTiming')"
      >
        <n-checkbox-group v-model:value="form.executionTiming" class="mt-[4px]">
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
  import { ref } from 'vue';
  import { NCheckbox, NCheckboxGroup, NForm, NFormItem, NInput, NSelect, useMessage } from 'naive-ui';

  import { FormDesignKeyEnum } from '@lib/shared/enums/formDesignEnum';
  import { useI18n } from '@lib/shared/hooks/useI18n';

  import { defaultBasicForm } from '@/config/process';

  const { t } = useI18n();
  // todo ts type
  const form = defineModel<Record<string, any>>('basicConfig', {
    default: () => ({
      ...defaultBasicForm,
    }),
  });

  const businessTypeOptions = [
    {
      label: t('crmFormCreate.drawer.quotation'),
      value: FormDesignKeyEnum.OPPORTUNITY_QUOTATION,
    },
    {
      label: t('module.contract'),
      value: FormDesignKeyEnum.CONTRACT,
    },
    {
      label: t('module.invoiceApproval'),
      value: FormDesignKeyEnum.INVOICE,
    },
    {
      label: t('module.order'),
      formKey: FormDesignKeyEnum.ORDER,
    },
  ];

  const executionTimingList = [
    {
      value: 'CREATE',
      label: t('common.create'),
    },
    {
      value: 'UPDATE',
      label: t('common.edit'),
    },
  ];
</script>

<style lang="less">
  .process-basic-setting-form {
    .n-form-item-label {
      font-weight: 600;
      color: var(--text-n1);
    }
  }
</style>
