<template>
  <n-form-item
    :label="props.fieldConfig.name"
    :path="props.path"
    :rule="props.fieldConfig.rules"
    :required="props.fieldConfig.rules.some((rule) => rule.key === 'required')"
    :label-placement="props.isSubTableField || props.isSubTableRender ? 'top' : props.formConfig?.labelPos"
  >
    <template #label>
      <div v-if="props.fieldConfig.showLabel" class="flex h-[22px] items-center gap-[4px] whitespace-nowrap">
        <div class="one-line-text">{{ props.fieldConfig.name }}</div>
        <CrmIcon v-if="props.fieldConfig.resourceFieldId" type="iconicon_correlation" />
      </div>
      <div v-else class="h-[22px]"></div>
    </template>
    <div
      v-if="props.fieldConfig.description"
      class="crm-form-create-item-desc"
      v-html="props.fieldConfig.description"
    ></div>
    <n-divider v-if="props.isSubTableField && !props.isSubTableRender" class="!my-0" />
    <n-date-picker
      v-model:value="value"
      :type="props.fieldConfig.dateType"
      :placeholder="props.fieldConfig.placeholder"
      :disabled="props.fieldConfig.editable === false || props.disabled || !!props.fieldConfig.resourceFieldId"
      class="w-full"
      @update-value="($event) => emit('change', $event)"
    >
    </n-date-picker>
  </n-form-item>
</template>

<script setup lang="ts">
  import { NDatePicker, NDivider, NFormItem } from 'naive-ui';

  import type { FormConfig } from '@lib/shared/models/system/module';

  import { FormCreateField } from '../../types';

  const props = defineProps<{
    fieldConfig: FormCreateField;
    formConfig?: FormConfig;
    path: string;
    needInitDetail?: boolean; // 判断是否编辑情况
    disabled?: boolean;
    isSubTableField?: boolean; // 是否是子表字段
    isSubTableRender?: boolean; // 是否是子表渲染
  }>();
  const emit = defineEmits<{
    (e: 'change', value: null | number | (string | number)[]): void;
  }>();

  const value = defineModel<null | number | [number, number]>('value', {
    default: null,
  });

  watch(
    () => props.fieldConfig.defaultValue,
    (val) => {
      if (!props.needInitDetail) {
        value.value = val !== undefined ? val : value.value;
        emit('change', value.value);
      }
    },
    {
      immediate: true,
    }
  );

  watch(
    () => props.fieldConfig.dateDefaultType,
    (val) => {
      if (val === 'current') {
        value.value = new Date().getTime();
      } else if (val === 'custom' && props.fieldConfig.defaultValue === null) {
        value.value = null;
      }
      emit('change', value.value);
    },
    {
      immediate: true,
    }
  );
</script>

<style lang="less" scoped></style>
