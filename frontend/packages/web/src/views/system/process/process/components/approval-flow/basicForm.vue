<template>
  <div class="process-basic-setting flex w-full justify-center pt-[40px]">
    <n-form ref="formRef" class="process-setting-form" :model="form" label-placement="top">
      <n-form-item require-mark-placement="left" path="formType" :label="t('process.process.basic.businessType')">
        <n-select
          v-model:value="form.formType"
          :options="businessTypeOptions"
          :disabled="props.needDetail"
          :placeholder="t('common.pleaseSelect')"
        />
      </n-form-item>
      <n-form-item
        require-mark-placement="right"
        path="name"
        :label="t('process.process.processName')"
        :rule="[
          {
            required: true,
            message: t('common.notNull', { value: `${t('process.process.processName')}` }),
            trigger: ['input'],
          },
        ]"
      >
        <n-input
          v-model:value="form.name"
          :maxlength="255"
          type="text"
          :disabled="readonly"
          :placeholder="t('common.pleaseInput')"
        />
      </n-form-item>
      <n-form-item
        require-mark-placement="right"
        path="executeTiming"
        required
        :label="t('process.process.basic.executionTiming')"
        :rule="[
          {
            validator: () => {
              if (form.createExecute || form.updateExecute || form.deleteExecute) {
                return true;
              }

              return new Error(t('common.notNull', { value: t('process.process.basic.executionTiming') }));
            },
            trigger: ['change'],
          },
        ]"
      >
        <div class="flex flex-col gap-[8px]">
          <n-checkbox
            v-for="item of executionTimingList"
            :key="item.value"
            v-model:checked="form[item.value as keyof BasicFormParams]"
            :disabled="readonly"
          >
            <div class="flex items-center gap-[8px]">
              {{ item.label }}
              <n-tooltip v-if="item.value === 'updateExecute'" trigger="hover" :delay="300">
                <template #trigger>
                  <CrmIcon
                    type="iconicon_help_circle"
                    :size="16"
                    class="cursor-pointer text-[var(--text-n4)] hover:text-[var(--primary-1)]"
                  />
                </template>
                {{ t('process.process.basic.updateExecuteTip') }}
              </n-tooltip>
            </div>
          </n-checkbox>
        </div>
      </n-form-item>
      <n-form-item require-mark-placement="right" path="description" :label="t('process.process.basic.description')">
        <n-input
          v-model:value="form.description"
          :disabled="readonly"
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
  import { NCheckbox, NForm, NFormItem, NInput, NSelect, NTooltip } from 'naive-ui';

  import { useI18n } from '@lib/shared/hooks/useI18n';
  import { BasicFormParams } from '@lib/shared/models/system/process';

  import CrmIcon from '@/components/pure/crm-icon-font/index.vue';

  import { businessTypeOptions, defaultBasicForm, executionTimingList } from '@/config/process';

  import type { FormInst } from 'naive-ui';

  const { t } = useI18n();
  const props = defineProps<{
    needDetail?: boolean;
    readonly?: boolean;
  }>();

  const form = defineModel<BasicFormParams>('basicConfig', {
    default: () => ({
      ...defaultBasicForm,
    }),
  });

  const formRef = ref<FormInst | null>(null);

  async function validate(cb?: () => void) {
    try {
      await formRef.value?.validate();
      cb?.();
      return true;
    } catch {
      return false;
    }
  }

  defineExpose({
    validate,
  });
</script>

<style scoped lang="less">
  .process-setting-form {
    width: 540px;
    :deep(.n-form-item-label) {
      font-weight: 600;
    }
  }
</style>
